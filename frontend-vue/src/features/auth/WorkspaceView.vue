<script setup>
import { useRouter } from "vue-router";
import { useSessionStore } from "../../stores/session";
const router = useRouter(); const session = useSessionStore();
async function logout() { await session.logout(); router.replace("/"); }
</script>
<template><main class="workspace"><p class="eyebrow">SESSION ACTIVE</p><h1>你好，{{ session.user?.name }}</h1><p>已通过 {{ session.mode === "api" ? "Java API 与本地 MySQL" : "Mock" }} 建立会话。此第一切片只验证认证边界；其余业务仍请在旧版体验。</p><button class="primary" @click="logout">退出登录</button></main></template>
