<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useChatStore } from "../../stores/chat";
import { useSessionStore } from "../../stores/session";
import {
  createImageAttachments,
  formatFileSize,
  isImageAttachment,
  isPreviewableImage
} from "./image-attachments";
import {
  captureScrollPosition,
  restoreScrollPosition,
  scrollToLatest
} from "./scroll-position";

const chat = useChatStore();
const route = useRoute();
const session = useSessionStore();
const keyword = ref("");
const draft = ref("");
const attachments = ref([]);
const messageStream = ref(null);
const contactsOpen = ref(true);
const addingImages = ref(false);
const preview = ref(null);

onMounted(async () => {
  await chat.initialize();
  await preloadConversationImages();
  await nextTick();
  scrollToLatest(messageStream.value);
});

onUnmounted(() => chat.clearAttachmentUrls());

watch(
  () => [chat.selectedId, chat.conversations[chat.selectedId]],
  () => { void preloadConversationImages(); }
);

async function search() { await chat.search(keyword.value); }

async function send() {
  try {
    await chat.send(draft.value, attachments.value);
    draft.value = "";
    attachments.value = [];
    await nextTick();
    scrollToLatest(messageStream.value);
  } catch (error) {
    chat.notice = error.message;
  }
}

async function selectConversation(friendId) {
  await chat.select(friendId);
  if (window.matchMedia("(max-width: 760px)").matches) contactsOpen.value = false;
  await preloadConversationImages();
  await nextTick();
  scrollToLatest(messageStream.value);
}

async function loadOlderMessages() {
  const position = captureScrollPosition(messageStream.value);
  await chat.loadMessages(chat.selectedId, chat.paging[chat.selectedId].nextBeforeId);
  await preloadConversationImages();
  await nextTick();
  restoreScrollPosition(messageStream.value, position);
}

async function addImages(event) {
  addingImages.value = true;
  try {
    attachments.value = [...attachments.value, ...await createImageAttachments(event.target.files)];
  } catch (error) {
    chat.notice = error.message;
  } finally {
    event.target.value = "";
    addingImages.value = false;
  }
}

async function preloadConversationImages() {
  const peerId = chat.selectedId;
  if (!peerId) return;
  const messages = chat.conversations[peerId] || [];
  await Promise.all(messages.flatMap((message) => (message.attachments || [])
    .filter(isPreviewableImage)
    .filter((attachment) => !chat.attachmentUrl(peerId, attachment.id))
    .map((attachment) => chat.loadAttachmentUrl(peerId, attachment.id))));
}

function imageSource(attachment) {
  return chat.attachmentUrl(chat.selectedId, attachment.id);
}

async function openImage(attachment) {
  const source = imageSource(attachment)
    || await chat.loadAttachmentUrl(chat.selectedId, attachment.id);
  if (source) preview.value = { name: attachment.name, source };
}

function isImageOnlyMessage(message) {
  return message.text === "图片" && message.attachments?.some(isImageAttachment);
}
</script>
<template>
  <section class="chat-workspace">
    <p v-if="chat.notice" class="chat-notice">{{ chat.notice }}</p>
    <div class="chat-grid">
      <aside v-show="contactsOpen" class="contact-pane">
        <form class="search-box" @submit.prevent="search"><input v-model="keyword" placeholder="搜索姓名或手机号" /><button>搜索</button></form>
        <div v-if="chat.results.length" class="search-results"><article v-for="person in chat.results" :key="person.id"><span>{{ person.name.slice(0, 1) }}</span><div><strong>{{ person.name }}</strong><small>{{ person.role }}</small></div><button @click="chat.requestFriend(person.id)">申请</button></article></div>
        <section class="request-list"><p>好友申请</p><article v-for="request in chat.requests" :key="request.id" :class="{ 'is-target': String(request.id) === String(route.query.request || '') }"><span>{{ request.user?.name?.slice(0, 1) || "?" }}</span><div><strong>{{ request.user?.name }}</strong><small>{{ request.status === "pending" ? "待处理" : request.status }}</small></div><template v-if="request.direction === 'incoming' && request.status === 'pending'"><button @click="chat.resolveRequest(request.id, 'accept')">同意</button><button @click="chat.resolveRequest(request.id, 'reject')">拒绝</button></template></article></section>
        <section class="conversation-list"><p>联系人 {{ chat.friends.length }}</p><button v-for="friend in chat.friends" :key="friend.id" class="conversation-button" :class="{ active: friend.id === chat.selectedId }" @click="selectConversation(friend.id)"><span>{{ friend.name.slice(0, 1) }}</span><div><strong>{{ friend.name }}</strong><small>{{ chat.previews[friend.id]?.text || "暂无消息" }}</small></div><b v-if="chat.unread[friend.id]">{{ chat.unread[friend.id] }}</b></button></section>
      </aside>
      <section v-if="chat.selectedFriend" class="message-pane">
        <header>
          <div><p class="eyebrow">PRIVATE CONVERSATION</p><h3>{{ chat.selectedFriend.name }}</h3></div>
          <div class="conversation-actions"><span>{{ chat.selectedFriend.status || "在线" }}</span><button type="button" class="chat-contact-toggle" :aria-expanded="contactsOpen" @click="contactsOpen = !contactsOpen">{{ contactsOpen ? "收起联系人" : "联系人" }}</button></div>
        </header>
        <button v-if="chat.paging[chat.selectedId]?.hasMore" class="older" @click="loadOlderMessages">加载更早消息</button>
        <div ref="messageStream" class="message-stream">
          <article v-for="message in chat.conversations[chat.selectedId] || []" :key="message.id" :class="{ mine: message.from === session.user?.id }">
            <p v-if="message.deleted">消息已撤回</p>
            <p v-else-if="!isImageOnlyMessage(message)">{{ message.text }}</p>
            <div v-if="!message.deleted && message.attachments?.length" class="attachment-row">
              <template v-for="file in message.attachments" :key="file.id">
                <button v-if="isPreviewableImage(file)" type="button" class="message-image" :aria-label="`查看图片：${file.name}`" @click="openImage(file)">
                  <img v-if="imageSource(file)" :src="imageSource(file)" :alt="file.name" />
                  <span v-else>图片不可用，请重新发送</span>
                </button>
                <span v-else>{{ file.name }} · {{ formatFileSize(file.size || 0) }}</span>
              </template>
            </div>
            <small>{{ message.time }}</small>
            <button v-if="message.from === session.user?.id && !message.deleted" @click="chat.withdraw(message.id)">撤回</button>
          </article>
        </div>
        <form class="composer" @submit.prevent="send">
          <div v-if="attachments.length" class="pending-files">
            <article v-for="file in attachments" :key="file.id" class="pending-image">
              <img :src="file.dataUrl" :alt="file.name" />
              <button type="button" :aria-label="`移除图片：${file.name}`" @click="attachments = attachments.filter((item) => item.id !== file.id)">×</button>
            </article>
          </div>
          <textarea v-model="draft" placeholder="输入消息…" rows="2"></textarea>
          <footer><label class="file-button">添加图片<input type="file" accept="image/png,image/jpeg,image/webp,image/gif" multiple :disabled="addingImages" @change="addImages" /></label><button class="send-button" :disabled="addingImages">{{ addingImages ? "读取图片…" : "发送" }}</button></footer>
        </form>
      </section>
      <section v-else class="message-empty">选择一个已建立好友关系的联系人开始聊天。</section>
    </div>
    <div v-if="preview" class="image-lightbox" role="dialog" aria-modal="true" :aria-label="preview.name" @click.self="preview = null">
      <button type="button" class="image-lightbox-close" aria-label="关闭图片预览" @click="preview = null">×</button>
      <img :src="preview.source" :alt="preview.name" />
      <p>{{ preview.name }}</p>
    </div>
  </section>
</template>
