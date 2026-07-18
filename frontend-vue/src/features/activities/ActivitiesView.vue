<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useActivityStore } from "../../stores/activity";

const a = useActivityStore();
const route = useRoute();
const draft = ref({ title: "", category: "", capacity: 40, location: "", startsAt: "", endsAt: "", description: "" });

onMounted(a.load);

async function submit() {
  const created = await a.create({ ...draft.value, capacity: Number(draft.value.capacity) });
  if (created) draft.value = { title: "", category: "", capacity: 40, location: "", startsAt: "", endsAt: "", description: "" };
}
</script>

<template>
  <section class="activity-workspace">
    <header><div><p class="eyebrow">CAMPUS ACTIVITIES</p><h2>校园活动</h2></div></header>
    <form class="activity-filters" @submit.prevent="a.load"><input v-model="a.filters.from" type="date"/><input v-model="a.filters.to" type="date"/><input v-model="a.filters.category" placeholder="活动类别"/><button>筛选</button></form>
    <p v-if="a.notice" class="feed-notice">{{ a.notice }}</p>

    <form v-if="a.isOrganizer" class="activity-create" @submit.prevent="submit"><header><div><p class="eyebrow">CREATE ACTIVITY</p><h3>提交活动审核</h3></div><span>组织者身份由当前账号确定</span></header><div class="activity-create-grid"><input v-model="draft.title" required maxlength="120" placeholder="活动标题"/><input v-model="draft.category" required maxlength="60" placeholder="类别"/><input v-model.number="draft.capacity" required type="number" min="1" max="10000" placeholder="名额"/><input v-model="draft.location" required maxlength="160" placeholder="地点"/><label>开始时间<input v-model="draft.startsAt" required type="datetime-local"/></label><label>结束时间<input v-model="draft.endsAt" required type="datetime-local"/></label><textarea v-model="draft.description" required maxlength="2000" placeholder="说明活动内容、参与方式和准备事项。"></textarea></div><button>提交审核</button></form>

    <article v-for="item in a.items" :key="item.id" class="activity-card" :class="{ 'is-target': String(item.id) === String(route.query.activity || '') }"><header><span>{{ item.category }}</span><h3>{{ item.title }}</h3></header><p>{{ item.description }}</p><small>{{ item.startsAt }} · {{ item.location }} · {{ item.capacity }} 人</small><footer><template v-if="a.registrations[item.id]?.status === 'registered'"><strong>已报名</strong><button @click="a.cancel(item.id)">取消报名</button></template><template v-else-if="a.registrations[item.id]?.status === 'waitlisted'"><strong>候补中</strong><button @click="a.cancel(item.id)">取消候补</button></template><button v-else @click="a.register(item.id)">立即报名</button></footer></article>

    <section v-if="a.managed.length" class="managed"><p class="eyebrow">MY ACTIVITY OPERATIONS</p><article v-for="item in a.managed" :key="item.id"><strong>{{ item.title }}</strong><small v-if="item.reviewDecision === 'pending'">等待管理员审核</small><button @click="a.roster(item.id)">查看名单</button><div v-if="a.rosters[item.id]"><p v-for="entry in a.rosters[item.id].entries" :key="entry.id">{{ entry.name }} / {{ entry.status }} <button v-if="entry.status === 'registered'" @click="a.checkIn(item.id, entry.id)">签到</button></p></div></article></section>
  </section>
</template>
