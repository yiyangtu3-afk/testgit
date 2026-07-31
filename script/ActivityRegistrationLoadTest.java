import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Runs a controlled concurrent registration scenario against an explicitly
 * selected API. Run with Java 21 source-file mode; no project dependency is
 * required.
 */
public class ActivityRegistrationLoadTest {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  public static void main(String[] args) throws Exception {
    Settings settings = Settings.parse(args);
    if (settings.help()) {
      usage();
      return;
    }
    settings.validate();
    List<String> tokens = readTokens(settings.tokensFile());
    if (tokens.isEmpty()) {
      throw new IllegalArgumentException("令牌文件不包含可用的 bearer token");
    }

    HttpClient client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    List<Attempt> attempts = run(client, settings, tokens);
    report(settings, attempts);

    long unexpected = attempts.stream().filter(Attempt::unexpected).count();
    if (unexpected > 0) {
      System.exit(2);
    }
  }

  private static List<Attempt> run(HttpClient client, Settings settings, List<String> tokens)
      throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    CountDownLatch ready = new CountDownLatch(tokens.size());
    CountDownLatch start = new CountDownLatch(1);
    Semaphore permits = new Semaphore(settings.effectiveConcurrency(tokens.size()));
    try {
      List<Future<Attempt>> futures = tokens.stream().map(token -> executor.submit(() -> {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
          throw new IllegalStateException("并发压测未能同时开始");
        }
        permits.acquire();
        try {
          return register(client, settings, token);
        } finally {
          permits.release();
        }
      })).toList();
      if (!ready.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("压测线程未能全部就绪");
      }
      start.countDown();
      List<Attempt> attempts = new ArrayList<>(futures.size());
      for (Future<Attempt> future : futures) {
        attempts.add(future.get(REQUEST_TIMEOUT.toSeconds() + 5, TimeUnit.SECONDS));
      }
      return attempts;
    } finally {
      executor.shutdownNow();
    }
  }

  private static Attempt register(HttpClient client, Settings settings, String token)
      throws Exception {
    long startedAt = System.nanoTime();
    HttpRequest request = HttpRequest.newBuilder(settings.registrationUri())
        .timeout(REQUEST_TIMEOUT)
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", "load-" + UUID.randomUUID())
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    return new Attempt(response.statusCode(), registrationStatus(response.body()), elapsedMillis);
  }

  private static void report(Settings settings, List<Attempt> attempts) {
    Map<String, Long> outcomes = new LinkedHashMap<>();
    for (Attempt attempt : attempts) {
      outcomes.merge(attempt.label(), 1L, Long::sum);
    }
    List<Long> latencies = attempts.stream().map(Attempt::elapsedMillis).sorted().toList();
    System.out.printf("Activity registration load test: %d attempts, concurrency=%d%n",
        attempts.size(), settings.effectiveConcurrency(attempts.size()));
    outcomes.forEach((label, count) -> System.out.printf("  %s: %d%n", label, count));
    System.out.printf("  latency ms: p50=%d p95=%d max=%d%n", percentile(latencies, 0.50),
        percentile(latencies, 0.95), latencies.getLast());
    System.out.println("Only 201, 409, and 429 are expected outcomes for a controlled run.");
  }

  private static long percentile(List<Long> sortedValues, double percentile) {
    int index = Math.max(0, (int) Math.ceil(sortedValues.size() * percentile) - 1);
    return sortedValues.get(index);
  }

  private static List<String> readTokens(Path tokensFile) throws Exception {
    return Files.readAllLines(tokensFile).stream().map(String::trim)
        .filter(line -> !line.isBlank() && !line.startsWith("#"))
        .map(line -> line.startsWith("Bearer ") ? line.substring("Bearer ".length()).trim() : line)
        .toList();
  }

  private static String registrationStatus(String body) {
    if (body.contains("\"status\":\"registered\"")) {
      return "registered";
    }
    if (body.contains("\"status\":\"waitlisted\"")) {
      return "waitlisted";
    }
    return null;
  }

  private static void usage() {
    System.out.println("""
        Usage:
          java script/ActivityRegistrationLoadTest.java \\
            --base-url http://127.0.0.1:18084 \\
            --activity-id ACTIVITY_ID \\
            --tokens-file /absolute/path/test-student-tokens.txt \\
            --concurrency 6

        The token file has one student JWT per line. This tool sends one real
        registration per token, creates normal business records, and never starts
        services or changes database data outside the API request itself.

        Use an isolated Compose activity and test accounts. A non-loopback URL
        also requires --allow-remote.
        """);
  }

  private record Attempt(int httpStatus, String registrationStatus, long elapsedMillis) {
    private String label() {
      return registrationStatus == null ? Integer.toString(httpStatus)
          : httpStatus + " " + registrationStatus;
    }

    private boolean unexpected() {
      return httpStatus != 201 && httpStatus != 409 && httpStatus != 429;
    }
  }

  private record Settings(URI baseUrl, String activityId, Path tokensFile, int concurrency,
                          boolean allowRemote, boolean help) {
    private static Settings parse(String[] args) {
      Map<String, String> values = new LinkedHashMap<>();
      boolean allowRemote = false;
      boolean help = false;
      for (int index = 0; index < args.length; index++) {
        String argument = args[index];
        if ("--allow-remote".equals(argument)) {
          allowRemote = true;
        } else if ("--help".equals(argument) || "-h".equals(argument)) {
          help = true;
        } else if (argument.startsWith("--") && index + 1 < args.length) {
          values.put(argument, args[++index]);
        } else {
          throw new IllegalArgumentException("无法识别参数: " + argument);
        }
      }
      String rawBaseUrl = values.get("--base-url");
      URI baseUrl = rawBaseUrl == null ? null : URI.create(trimTrailingSlash(rawBaseUrl));
      String activityId = values.get("--activity-id");
      String rawTokensFile = values.get("--tokens-file");
      Path tokensFile = rawTokensFile == null ? null : Path.of(rawTokensFile);
      int concurrency = values.containsKey("--concurrency")
          ? Integer.parseInt(values.get("--concurrency")) : 0;
      return new Settings(baseUrl, activityId, tokensFile, concurrency, allowRemote, help);
    }

    private void validate() {
      if (baseUrl == null || activityId == null || activityId.isBlank() || tokensFile == null) {
        throw new IllegalArgumentException("必须提供 --base-url、--activity-id 和 --tokens-file");
      }
      if (!"http".equals(baseUrl.getScheme()) && !"https".equals(baseUrl.getScheme())) {
        throw new IllegalArgumentException("--base-url 必须是 HTTP(S) 地址");
      }
      String host = baseUrl.getHost();
      if (host == null || host.isBlank()) {
        throw new IllegalArgumentException("--base-url 必须包含主机名");
      }
      boolean loopback = "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
          || "::1".equals(host);
      if (!loopback && !allowRemote) {
        throw new IllegalArgumentException("非回环地址必须显式提供 --allow-remote");
      }
      if (concurrency < 0) {
        throw new IllegalArgumentException("--concurrency 必须为正整数");
      }
      if (!Files.isRegularFile(tokensFile)) {
        throw new IllegalArgumentException("--tokens-file 必须是已有文件");
      }
    }

    private URI registrationUri() {
      return baseUrl.resolve("/api/activities/" + activityId + "/registrations");
    }

    private int effectiveConcurrency(int attemptCount) {
      return concurrency == 0 ? attemptCount : Math.min(concurrency, attemptCount);
    }

    private static String trimTrailingSlash(String value) {
      return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
  }
}
