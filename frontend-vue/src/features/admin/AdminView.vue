<script setup>
import { computed, onMounted, ref } from "vue";
import { useAdminStore } from "../../stores/admin";

const admin = useAdminStore();
const filter = ref("all"); const activityReasons = ref({}); const reportRange = ref("today");
const visibleModeration = computed(() => admin.moderation.filter((item) => filter.value === "all" || item.type === filter.value));
onMounted(() => admin.load());
function label(type) { return type === "post" ? "动态" : "评论"; }
function downloadReport() {
  if (!admin.report) return;
  const rows = [["CampusLink 管理报表"], ["范围", admin.report.range.label], ...Object.entries(admin.report.metrics), [], ["时间", "模块", "事件"], ...admin.report.auditEvents.map((item) => [item.time, item.module, item.event])];
  const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
  const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" })); link.download = admin.report.fileName; link.click(); URL.revokeObjectURL(link.href);
}
function deleteModeration() { if (window.confirm(`确认删除选中的 ${admin.selectedModeration.length} 条审核记录吗？`)) admin.deleteModeration(); }
function deleteAudits() { if (window.confirm(`确认删除选中的 ${admin.selectedAudits.length} 条审计记录吗？`)) admin.deleteAudits(); }
</script>

<template>
  <section class="admin-workspace">
    <header class="admin-heading"><div><p class="eyebrow">ADMINISTRATION / REVIEW DESK</p><h2>管理控制台</h2></div><button @click="admin.load">刷新数据</button></header>
    <section v-if="!admin.isAdmin" class="admin-denied"><p class="eyebrow">ADMIN ONLY</p><h3>需要管理员权限</h3><p>请使用教务管理员账号进入审核、审计和报表工作区。</p></section>
    <template v-else>
      <p v-if="admin.notice" class="feed-notice">{{ admin.notice }}</p>
      <div class="metric-grid"><article v-for="(value, key) in admin.metrics" :key="key"><p>{{ key }}</p><strong>{{ value }}</strong></article></div>

      <section class="admin-panel activity-review-panel"><header><div><p class="eyebrow">ACTIVITY REVIEW</p><h3>待审核活动</h3></div><strong>{{ admin.activities.length }}</strong></header><p v-if="!admin.activities.length" class="admin-empty">当前没有待审核活动。</p><article v-for="item in admin.activities" :key="item.id" class="admin-review-card"><div><span>{{ item.category }}</span><h4>{{ item.title }}</h4><p>{{ item.description }}</p><small>{{ item.organizerName }} · {{ item.startsAt }} · {{ item.location }}</small></div><footer><input v-model="activityReasons[item.id]" placeholder="拒绝原因（拒绝时必填）"/><button @click="admin.reviewActivity(item.id, 'approve')">同意</button><button class="danger" @click="admin.reviewActivity(item.id, 'reject', activityReasons[item.id])">拒绝</button></footer></article></section>

      <section class="admin-panel"><header><div><p class="eyebrow">CONTENT REVIEW</p><h3>待审核内容</h3></div><strong>{{ admin.moderation.length }}</strong></header><div class="admin-toolbar"><div><button :class="{ active: filter === 'all' }" @click="filter = 'all'">全部</button><button :class="{ active: filter === 'post' }" @click="filter = 'post'">动态</button><button :class="{ active: filter === 'comment' }" @click="filter = 'comment'">评论</button></div><button class="danger-outline" :disabled="!admin.selectedModeration.length" @click="deleteModeration">删除所选 {{ admin.selectedModeration.length || '' }}</button></div><p v-if="!visibleModeration.length" class="admin-empty">当前筛选下没有待审核内容。</p><article v-for="item in visibleModeration" :key="item.id" class="admin-review-card"><label class="admin-check"><input type="checkbox" :checked="admin.selectedModeration.includes(item.id)" @change="admin.selectedModeration = admin.toggle(admin.selectedModeration, item.id)"/> 选择</label><div><span>{{ label(item.type) }}</span><h4>{{ item.title || item.body }}</h4><p>{{ item.body }}</p><small>{{ item.author }} · {{ item.submittedAt || item.time }}</small></div><footer><button @click="admin.resolveModeration(item.id, 'approve')">同意</button><button class="danger" @click="admin.resolveModeration(item.id, 'reject')">拒绝</button></footer></article></section>

      <section class="admin-panel"><header><div><p class="eyebrow">AUDIT LOG</p><h3>审计记录</h3></div><button class="danger-outline" :disabled="!admin.selectedAudits.length" @click="deleteAudits">删除所选 {{ admin.selectedAudits.length || '' }}</button></header><div class="audit-table"><article v-for="item in admin.audits" :key="item.id"><label><input type="checkbox" :checked="admin.selectedAudits.includes(item.id)" @change="admin.selectedAudits = admin.toggle(admin.selectedAudits, item.id)"/></label><time>{{ item.time }}</time><span>{{ item.module }}</span><p>{{ item.event }}</p></article><p v-if="!admin.audits.length" class="admin-empty">当前没有审计记录。</p></div></section>

      <section class="admin-panel report-panel"><header><div><p class="eyebrow">ADMIN REPORT</p><h3>报表中心</h3></div></header><div class="admin-toolbar"><select v-model="reportRange"><option value="today">今日</option><option value="week">本周</option><option value="all">全部</option></select><button @click="admin.generateReport(reportRange)">生成报表</button></div><div v-if="admin.report" class="report-result"><p><strong>{{ admin.report.range.label }}</strong> · {{ admin.report.generatedAt }} 生成</p><div><span v-for="(value, key) in admin.report.metrics" :key="key">{{ key }} {{ value }}</span></div><button @click="downloadReport">下载 CSV</button></div></section>
    </template>
  </section>
</template>
