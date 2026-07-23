<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useActivityStore } from "../../stores/activity";

const a = useActivityStore();
const route = useRoute();
const draft = ref({ title: "", category: "", capacity: 40, location: "", startsAt: "", endsAt: "", description: "" });
const verificationCodes = ref({});
const operationsSection = ref(null);

onMounted(a.load);

async function submit() {
  const created = await a.create({ ...draft.value, capacity: Number(draft.value.capacity) });
  if (created) draft.value = { title: "", category: "", capacity: 40, location: "", startsAt: "", endsAt: "", description: "" };
}

async function verifyCredential(activityId) {
  const entry = await a.verifyCredential(activityId, verificationCodes.value[activityId] || "");
  if (entry) verificationCodes.value[activityId] = "";
}

function openFieldCheckIn() {
  operationsSection.value?.scrollIntoView({ behavior: "smooth", block: "start" });
  requestAnimationFrame(() => operationsSection.value?.querySelector("input")?.focus());
}

function canCheckIn(item) {
  return item.status === "published" || item.status === "full";
}

function checkInUnavailableReason(item) {
  return item.reviewDecision === "pending"
    ? "活动正在等待管理员审核，通过后才能现场签到。"
    : "活动当前不可现场签到。";
}
</script>

<template>
  <section class="activity-workspace">
    <header class="activities-heading"><div><p class="eyebrow">CAMPUS ACTIVITIES</p><h2>校园活动</h2></div><button v-if="a.isOrganizer" class="field-check-in-link" @click="openFieldCheckIn"><span>现场签到</span><small>核验学生凭证 ↓</small></button></header>
    <form class="activity-filters" @submit.prevent="a.load"><input v-model="a.filters.from" type="date"/><input v-model="a.filters.to" type="date"/><input v-model="a.filters.category" placeholder="活动类别"/><button>筛选</button></form>
    <p v-if="a.notice" class="feed-notice">{{ a.notice }}</p>

    <form v-if="a.isOrganizer" class="activity-create" @submit.prevent="submit"><header><div><p class="eyebrow">CREATE ACTIVITY</p><h3>提交活动审核</h3></div><span>组织者身份由当前账号确定</span></header><div class="activity-create-grid"><input v-model="draft.title" required maxlength="120" placeholder="活动标题"/><input v-model="draft.category" required maxlength="60" placeholder="类别"/><input v-model.number="draft.capacity" required type="number" min="1" max="10000" placeholder="名额"/><input v-model="draft.location" required maxlength="160" placeholder="地点"/><label>开始时间<input v-model="draft.startsAt" required type="datetime-local"/></label><label>结束时间<input v-model="draft.endsAt" required type="datetime-local"/></label><textarea v-model="draft.description" required maxlength="2000" placeholder="说明活动内容、参与方式和准备事项。"></textarea></div><button>提交审核</button></form>

    <article v-for="item in a.items" :key="item.id" class="activity-card" :class="{ 'is-target': String(item.id) === String(route.query.activity || '') }"><header><span>{{ item.category }}</span><h3>{{ item.title }}</h3></header><p>{{ item.description }}</p><small>{{ item.startsAt }} · {{ item.location }} · {{ item.capacity }} 人 · 发起人：{{ item.organizerName || "未标注" }}</small><footer><template v-if="a.registrations[item.id]?.status === 'registered'"><strong>已报名</strong><button @click="a.credential(item.id)">展示签到凭证</button><button class="quiet" @click="a.cancel(item.id)">取消报名</button></template><template v-else-if="a.registrations[item.id]?.status === 'waitlisted'"><strong>候补中</strong><button @click="a.cancel(item.id)">取消候补</button></template><strong v-else-if="a.registrations[item.id]?.status === 'checked_in'">已签到</strong><button v-else-if="a.isStudent" @click="a.register(item.id)">立即报名</button><span v-else class="registration-restricted">仅学生可报名</span></footer><aside v-if="a.credentials[item.id]" class="attendance-pass"><div><p>YOUR ATTENDANCE PASS</p><strong>{{ a.credentials[item.id].code }}</strong><small>此凭证会在再次展示时更新。仅向活动组织者出示。</small></div><span aria-hidden="true">✓</span></aside></article>

    <section v-if="a.isOrganizer" ref="operationsSection" class="managed"><header class="operations-heading"><div><p class="eyebrow">MY ACTIVITY OPERATIONS</p><h3>现场签到</h3></div><p v-if="a.managed.length">输入学生出示的签到凭证，核验结果会即时写入活动名单。</p><p v-else>此账号目前没有可核验的活动。</p></header><p v-if="!a.managed.length" class="managed-empty">请切换到创建该活动的教师或社团负责人账号，再核验学生展示的签到凭证。</p><article v-for="item in a.managed" :key="item.id"><header><div><strong>{{ item.title }}</strong><small v-if="item.reviewDecision === 'pending'">等待管理员审核</small></div><button v-if="canCheckIn(item)" @click="a.roster(item.id)">查看名单</button></header><p v-if="!canCheckIn(item)" class="check-in-unavailable">{{ checkInUnavailableReason(item) }}</p><template v-else><form class="credential-check" @submit.prevent="verifyCredential(item.id)"><label :for="`credential-${item.id}`">签到凭证核验</label><div><input :id="`credential-${item.id}`" v-model="verificationCodes[item.id]" required maxlength="128" autocomplete="off" placeholder="输入学生出示的签到凭证"/><button>核验签到</button></div><small>核验由服务端确认组织者身份、活动归属和报名状态。</small></form><div v-if="a.rosters[item.id]" class="roster-list"><p v-for="entry in a.rosters[item.id].entries" :key="entry.registrationId"><span>{{ entry.attendeeName }} / {{ entry.status }}</span><button v-if="entry.status === 'registered'" @click="a.checkIn(item.id, entry.registrationId)">手动签到</button></p></div></template></article></section>
  </section>
</template>

<style scoped>
.activity-card footer .quiet { border: 1px solid #9aafa0; background: transparent; color: #496757; }
.registration-restricted { color: #667d70; font-size: .78rem; }
.activities-heading { display: flex; align-items: end; justify-content: space-between; gap: 1rem; }
.field-check-in-link { display: grid; gap: .18rem; min-width: 8.8rem; padding: .65rem .85rem; border: 1px solid #b48a29; border-left: 4px solid #b48a29; background: #183d31; color: #fff; text-align: left; box-shadow: .28rem .28rem 0 #e8dcae; }
.field-check-in-link span { font-size: .9rem; font-weight: 800; }
.field-check-in-link small { color: #e6ce80; font-size: .67rem; }
.attendance-pass { display: flex; align-items: stretch; justify-content: space-between; gap: 1rem; margin-top: 1rem; padding: .9rem 1rem; border: 1px dashed #b88d2f; background: linear-gradient(135deg, #203e32 0 72%, #d9bd6c 72%); color: #f5f1df; }
.attendance-pass p { margin: 0 0 .45rem; color: #dfc56f; font: .6rem/1 ui-monospace, monospace; letter-spacing: .14em; }
.attendance-pass strong { display: block; max-width: 100%; overflow-wrap: anywhere; color: #fff; font: 700 clamp(.82rem, 2vw, 1rem)/1.35 ui-monospace, monospace; letter-spacing: .04em; }
.attendance-pass small { max-width: 33rem; margin: .5rem 0 0; color: #d5e1d8; line-height: 1.45; }
.attendance-pass > span { display: grid; align-self: center; width: 2.1rem; height: 2.1rem; place-items: center; border: 1px solid rgba(255,255,255,.72); border-radius: 50%; background: #d9bd6c; color: #203e32; font-weight: 800; }
.managed article > header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.managed article > header > div { display: grid; gap: .1rem; }
.operations-heading { display: flex; align-items: end; justify-content: space-between; gap: 1rem; padding: 1rem 1rem .9rem; border-top: 3px solid #b88d2f; background: #183d31; color: #f3f6ed; }
.operations-heading .eyebrow { margin: 0; color: #e6ce80; }
.operations-heading h3 { margin: .3rem 0 0; font-size: 1.35rem; }
.operations-heading > p { max-width: 20rem; margin: 0; color: #d1dfd2; font-size: .78rem; line-height: 1.55; text-align: right; }
.managed-empty { margin: 0; padding: .85rem 1rem; border: 1px solid #d8e0d4; border-top: 0; background: #f4f7f1; color: #486052; line-height: 1.55; }
.check-in-unavailable { margin: .8rem 0 0; padding: .7rem .8rem; border-left: 3px solid #b88d2f; background: #f6f1df; color: #6b582b; line-height: 1.5; }
.credential-check { margin-top: 1rem; padding: .75rem .85rem; border-left: 3px solid #c89c35; background: #eef3ed; }
.credential-check label { display: block; color: #355747; font-size: .76rem; font-weight: 700; }
.credential-check div { display: flex; gap: .5rem; margin-top: .45rem; }
.credential-check input { min-width: 0; flex: 1; padding: .48rem .55rem; border: 1px solid #adbdaf; background: #fff; font: inherit; }
.credential-check small { margin: .5rem 0 0; color: #61796c; line-height: 1.45; }
.roster-list { margin-top: .75rem; }
.roster-list p { display: flex; align-items: center; justify-content: space-between; gap: .8rem; }
.roster-list p:last-child { border-bottom: 0; }
@media (max-width: 760px) { .attendance-pass { background: linear-gradient(135deg, #203e32 0 84%, #d9bd6c 84%); } .activities-heading, .operations-heading, .credential-check div, .managed article > header, .roster-list p { align-items: flex-start; flex-direction: column; } .field-check-in-link { width: 100%; } .operations-heading > p { text-align: left; } }
</style>
