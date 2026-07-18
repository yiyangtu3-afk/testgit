#!/usr/bin/env node

const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { join } = require("node:path");

const root = join(__dirname, "..");
const read = (path) => readFileSync(join(root, path), "utf8");

const dockerfile = read("Dockerfile.frontend");
const nginx = read("nginx.frontend.conf");
const defaultScript = read("script/run_frontend_demo.sh");
const legacyScript = read("script/run_legacy_frontend_demo.sh");
const login = read("frontend-vue/src/features/auth/LoginView.vue");

assert.match(dockerfile, /FROM node:22-alpine AS build/);
assert.match(dockerfile, /COPY --from=build \/app\/dist \/usr\/share\/nginx\/html/);
assert.match(dockerfile, /\/usr\/share\/nginx\/html\/legacy\//);
assert.match(nginx, /location \/api\/ \{[\s\S]*proxy_pass http:\/\/api:8080;/);
assert.match(nginx, /location \/ws\/ \{[\s\S]*Upgrade \$http_upgrade;/);
assert.match(nginx, /location \/ \{[\s\S]*try_files \$uri \$uri\/ \/index\.html;/);
assert.match(nginx, /location \/legacy\/ \{/);
assert.match(defaultScript, /cd "\$PROJECT_DIR\/frontend-vue"/);
assert.match(defaultScript, /npm run dev/);
assert.match(legacyScript, /python3 -m http\.server 5179/);
assert.match(login, /legacyHref/);

process.stdout.write("Vue default entry configuration passed.\n");
