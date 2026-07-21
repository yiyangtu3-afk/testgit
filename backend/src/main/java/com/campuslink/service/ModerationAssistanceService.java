package com.campuslink.service;

import com.campuslink.dto.DemoDtos.ModerationAssistanceView;
import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Produces explainable, non-binding guidance without calling an external model. */
@Service
public class ModerationAssistanceService {

  private static final Pattern PHONE_NUMBER = Pattern.compile("1[3-9]\\d{9}");
  private static final List<String> HIGH_RISK_TERMS = List.of("代写", "刷单", "赌博", "色情", "诈骗", "违法交易");
  private static final List<String> CONTACT_TERMS = List.of("加微信", "加vx", "扫码", "进群");

  public ModerationAssistanceView analyze(ModerationItemEntity item) {
    String body = item.body() == null ? "" : item.body().trim();
    String normalized = body.toLowerCase(Locale.ROOT);
    List<String> signals = new ArrayList<>();

    if (PHONE_NUMBER.matcher(body).find()) {
      signals.add("包含疑似手机号");
    }
    addMatchedTerms(normalized, HIGH_RISK_TERMS, signals, "命中高风险词：");
    addMatchedTerms(normalized, CONTACT_TERMS, signals, "包含引流提示：");

    boolean highRisk = signals.stream().anyMatch(signal -> signal.startsWith("命中高风险词"));
    if (highRisk) {
      return new ModerationAssistanceView(
          "reject",
          "high",
          signals,
          "建议拒绝：内容出现高风险信号，请管理员核实上下文后填写审核意见。",
          "local-policy-v1");
    }
    if (!signals.isEmpty()) {
      return new ModerationAssistanceView(
          "manual_review",
          "medium",
          signals,
          "建议人工复核：请确认联系方式或引流信息是否符合校园内容规范。",
          "local-policy-v1");
    }
    return new ModerationAssistanceView(
        "approve",
        "low",
        List.of("未检测到预设高风险信号"),
        "建议通过：仍请管理员结合内容语境作出最终决定。",
        "local-policy-v1");
  }

  private void addMatchedTerms(
      String body,
      List<String> terms,
      List<String> signals,
      String prefix) {
    terms.stream()
        .filter(body::contains)
        .forEach(term -> signals.add(prefix + term));
  }
}
