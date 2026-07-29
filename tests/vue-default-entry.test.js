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
const viteConfig = read("frontend-vue/vite.config.js");

assert.match(dockerfile, /FROM node:22-alpine AS build/);
assert.match(dockerfile, /COPY --from=build \/app\/dist \/usr\/share\/nginx\/html/);
assert.match(dockerfile, /\/usr\/share\/nginx\/html\/legacy\//);
assert.match(nginx, /location \/api\/ \{[\s\S]*proxy_pass http:\/\/gateway:8081;/);
assert.match(nginx, /location \/ws\/ \{[\s\S]*Upgrade \$http_upgrade;/);
assert.equal((nginx.match(/proxy_set_header X-Forwarded-For \$remote_addr;/g) || []).length, 2);
assert.doesNotMatch(nginx, /proxy_add_x_forwarded_for/);
assert.match(nginx, /location \/ \{[\s\S]*try_files \$uri \$uri\/ \/index\.html;/);
assert.match(nginx, /location \/legacy\/ \{/);
assert.match(defaultScript, /cd "\$PROJECT_DIR\/frontend-vue"/);
assert.match(defaultScript, /npm run dev/);
assert.match(viteConfig, /CAMPUSLINK_VITE_API_TARGET \|\| "http:\/\/127\.0\.0\.1:8080"/);
assert.match(viteConfig, /CAMPUSLINK_VITE_WS_TARGET \|\| "ws:\/\/127\.0\.0\.1:8080"/);
assert.match(legacyScript, /python3 -m http\.server 5179/);
assert.match(login, /legacyHref/);

process.stdout.write("Vue default entry configuration passed.\n");
