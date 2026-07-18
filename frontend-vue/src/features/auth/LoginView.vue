<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useSessionStore } from "../../stores/session";

const router = useRouter();
const session = useSessionStore();
const phone = ref("13800000001");
const code = ref("");
const error = ref("");

async function requestCode() {
  error.value = "";
  try {
    const response = await session.requestCode(phone.value.trim());
    code.value = response.code;
  } catch (reason) { error.value = reason.message || "验证码获取失败。"; }
}
async function login() {
  error.value = "";
  try { await session.login(phone.value.trim(), code.value.trim()); router.push("/workspace"); }
  catch (reason) { error.value = reason.message || "登录失败。"; }
}
async function demoLogin() {
  error.value = "";
  try { await session.demoLogin(); router.push("/workspace"); }
  catch (reason) { error.value = reason.message || "进入演示失败。"; }
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel"><p class="eyebrow">CAMPUSLINK / VUE PREVIEW</p><h1>连接正在<br />发生的校园。</h1><p>Vue 工作台已覆盖社交、活动、通知和管理员领域；根目录静态入口仍是默认演示基线。</p></section>
    <section class="login-panel"><p class="eyebrow">IDENTITY GATE</p><h2>欢迎回来</h2><p class="muted">使用验证码登录，或直接进入演示账号。</p>
      <form @submit.prevent="login"><label>手机号<input v-model="phone" inputmode="numeric" maxlength="11" /></label><label>验证码<div class="code-row"><input v-model="code" maxlength="6" /><button type="button" class="secondary" :disabled="session.busy" @click="requestCode">获取验证码</button></div></label><button class="primary" :disabled="session.busy">{{ session.busy ? "处理中…" : "验证码登录" }}</button></form>
      <button class="demo" :disabled="session.busy" @click="demoLogin">快速进入演示</button><p class="status" :class="{ error }">{{ error || session.feedback || "等待登录" }}</p><p class="mode">数据来源：{{ session.mode === "api" ? "Java API" : session.mode === "mock" ? "Mock（仅 API 不可达时）" : "尚未连接" }}</p>
    </section>
  </main>
</template>
