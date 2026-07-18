<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useChatStore } from "../../stores/chat";
import { useSessionStore } from "../../stores/session";
const chat = useChatStore(); const route = useRoute(); const session = useSessionStore(); const keyword = ref(""); const draft = ref(""); const attachments = ref([]);
onMounted(async () => { await chat.initialize(); });
async function search() { await chat.search(keyword.value); }
async function send() { try { await chat.send(draft.value, attachments.value); draft.value = ""; attachments.value = []; } catch (error) { chat.notice = error.message; } }
function addFiles(event) { attachments.value = [...attachments.value, ...[...event.target.files].map((file) => ({ id: crypto.randomUUID(), name: file.name, size: file.size, type: file.type, kind: file.type?.split("/")[0] || "文件" }))]; event.target.value = ""; }
function formatSize(size) { return size < 1024 ? `${size} B` : `${Math.ceil(size / 1024)} KB`; }
</script>
<template>
  <section class="chat-workspace">
    <header class="chat-heading"><div><p class="eyebrow">CONTACTS / CHAT</p><h2>联系人与对话</h2></div><span>{{ session.mode === "api" ? "实时连接可用" : "Mock 离线演示" }}</span></header>
    <p v-if="chat.notice" class="chat-notice">{{ chat.notice }}</p>
    <div class="chat-grid">
      <aside class="contact-pane">
        <form class="search-box" @submit.prevent="search"><input v-model="keyword" placeholder="搜索姓名或手机号" /><button>搜索</button></form>
        <div v-if="chat.results.length" class="search-results"><article v-for="person in chat.results" :key="person.id"><span>{{ person.name.slice(0, 1) }}</span><div><strong>{{ person.name }}</strong><small>{{ person.role }}</small></div><button @click="chat.requestFriend(person.id)">申请</button></article></div>
        <section class="request-list"><p>好友申请</p><article v-for="request in chat.requests" :key="request.id" :class="{ 'is-target': String(request.id) === String(route.query.request || '') }"><span>{{ request.user?.name?.slice(0, 1) || "?" }}</span><div><strong>{{ request.user?.name }}</strong><small>{{ request.status === "pending" ? "待处理" : request.status }}</small></div><template v-if="request.direction === 'incoming' && request.status === 'pending'"><button @click="chat.resolveRequest(request.id, 'accept')">同意</button><button @click="chat.resolveRequest(request.id, 'reject')">拒绝</button></template></article></section>
        <section class="conversation-list"><p>联系人 {{ chat.friends.length }}</p><button v-for="friend in chat.friends" :key="friend.id" class="conversation-button" :class="{ active: friend.id === chat.selectedId }" @click="chat.select(friend.id)"><span>{{ friend.name.slice(0, 1) }}</span><div><strong>{{ friend.name }}</strong><small>{{ chat.previews[friend.id]?.text || "暂无消息" }}</small></div><b v-if="chat.unread[friend.id]">{{ chat.unread[friend.id] }}</b></button></section>
      </aside>
      <section class="message-pane" v-if="chat.selectedFriend"><header><div><p class="eyebrow">PRIVATE CONVERSATION</p><h3>{{ chat.selectedFriend.name }}</h3></div><span>{{ chat.selectedFriend.status || "在线" }}</span></header><button v-if="chat.paging[chat.selectedId]?.hasMore" class="older" @click="chat.loadMessages(chat.selectedId, chat.paging[chat.selectedId].nextBeforeId)">加载更早消息</button><div class="message-stream"><article v-for="message in chat.conversations[chat.selectedId] || []" :key="message.id" :class="{ mine: message.from === session.user?.id }"><p>{{ message.deleted ? "消息已撤回" : message.text }}</p><div v-if="!message.deleted && message.attachments?.length" class="attachment-row"><span v-for="file in message.attachments" :key="file.id">{{ file.name }} · {{ formatSize(file.size || 0) }}</span></div><small>{{ message.time }}</small><button v-if="message.from === session.user?.id && !message.deleted" @click="chat.withdraw(message.id)">撤回</button></article></div><form class="composer" @submit.prevent="send"><div v-if="attachments.length" class="pending-files"><span v-for="file in attachments" :key="file.id">{{ file.name }} <button type="button" @click="attachments = attachments.filter((item) => item.id !== file.id)">×</button></span></div><textarea v-model="draft" placeholder="输入消息…" rows="2"></textarea><footer><label class="file-button">添加附件<input type="file" multiple @change="addFiles" /></label><button class="send-button">发送</button></footer></form></section>
      <section v-else class="message-empty">选择一个已建立好友关系的联系人开始聊天。</section>
    </div>
  </section>
</template>
