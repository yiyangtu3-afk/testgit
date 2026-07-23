import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { createApi } from "../services/api";
import { useSessionStore } from "./session";

export function createActivityStore({ api, getUser = () => null, setMode = () => {} } = {}) {
  return () => {
    const items = ref([]), registrations = ref({}), filters = ref({ from: "", to: "", category: "" });
    const managed = ref([]), rosters = ref({}), credentials = ref({}), notice = ref("");
    const isOrganizer = computed(() => {
      const role = getUser()?.role || "";
      return role.includes("教师") || role.includes("社团负责人");
    });
    const run = async (call) => {
      const result = await call();
      setMode(result.mode);
      return result.data;
    };
    const load = async () => {
      items.value = await run(() => api.activities(filters.value));
      const pairs = await Promise.all(items.value.map(async (activity) =>
        [activity.id, await run(() => api.current(activity.id))]));
      registrations.value = Object.fromEntries(pairs);
      managed.value = isOrganizer.value ? await run(() => api.managed()) : [];
    };
    const create = async (activity) => {
      if (activity.endsAt <= activity.startsAt) {
        notice.value = "活动结束时间必须晚于开始时间。";
        return null;
      }
      try {
        const created = await run(() => api.createActivity(activity));
        managed.value = [created, ...managed.value.filter((item) => item.id !== created.id)];
        notice.value = "活动已提交审核。管理员通过前，它不会出现在公开活动列表。";
        return created;
      } catch (error) {
        notice.value = error.message || "活动提交失败。";
        return null;
      }
    };
    const register = async (id) => {
      registrations.value = { ...registrations.value, [id]: await run(() => api.register(id)) };
      notice.value = "报名状态已更新。";
    };
    const cancel = async (id) => {
      registrations.value = { ...registrations.value, [id]: await run(() => api.cancel(id)) };
      delete credentials.value[id];
      notice.value = "已取消报名。";
    };
    const roster = async (id) => {
      rosters.value = { ...rosters.value, [id]: await run(() => api.roster(id)) };
    };
    const checkIn = async (id, registrationId) => {
      await run(() => api.checkIn(id, registrationId));
      await roster(id);
      notice.value = "参与者已签到。";
    };
    const credential = async (id) => {
      try {
        const value = await run(() => api.credential(id));
        credentials.value = { ...credentials.value, [id]: value };
        notice.value = "签到凭证已更新，请仅向活动组织者展示。";
      } catch (error) {
        notice.value = error.message || "无法获取签到凭证。";
      }
    };
    const verifyCredential = async (id, code) => {
      try {
        const entry = await run(() => api.verifyCredential(id, code));
        await roster(id);
        notice.value = `${entry.attendeeName || "参与者"}已完成签到。`;
        return entry;
      } catch (error) {
        notice.value = error.message || "签到凭证核验失败。";
        return null;
      }
    };
    return { items, registrations, filters, managed, rosters, credentials, notice, isOrganizer, load,
      create, register, cancel, roster, checkIn, credential, verifyCredential };
  };
}

export const useActivityStore = defineStore("activity", () => {
  const session = useSessionStore();
  const api = createApi({ getToken: () => session.token, getUser: () => session.user });
  return createActivityStore({ api, getUser: () => session.user,
    setMode: (mode) => { session.mode = mode; } })();
});
