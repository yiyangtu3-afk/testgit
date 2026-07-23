export function createMockActivityApi(getUser) {
  const activities = [{ id: "a-1", title: "校园开源协作日", category: "科技", location: "工程训练中心 A201", startsAt: "2026-08-16T09:00:00", endsAt: "2026-08-16T16:30:00", capacity: 80, organizerId: "u-2001", organizerName: "陈老师", status: "published", description: "面向全校同学的开源项目协作与成果交流活动。" }];
  const registrations = {};
  const credentialCodes = {};
  const credentialByRegistration = {};
  const user = () => getUser() || { id: "u-1001", role: "学生账号", name: "林一" };
  const organizer = () => (user().role || "").includes("教师") || (user().role || "").includes("社团负责人");
  const currentRegistration = (activityId) => registrations[`${user().id}:${activityId}`];

  return {
    async activities(filters) {
      return activities.filter((activity) => (!filters.category || activity.category === filters.category)
        && (!filters.from || activity.startsAt.slice(0, 10) >= filters.from)
        && (!filters.to || activity.startsAt.slice(0, 10) <= filters.to));
    },
    async createActivity(activity) {
      if (!organizer()) throw new Error("只有教师或社团负责人可以创建活动");
      const created = { id: `a-${Date.now()}`, ...activity, capacity: Number(activity.capacity), organizerId: user().id, organizerName: user().name, status: "pending", reviewDecision: "pending", createdAt: new Date().toISOString() };
      activities.unshift(created);
      return created;
    },
    async managed() { return activities.filter((activity) => activity.organizerId === user().id); },
    async current(id) { return currentRegistration(id) || null; },
    async register(id) {
      const activity = activities.find((item) => item.id === id);
      const count = Object.values(registrations).filter((item) => item.activityId === id
        && ["registered", "checked_in"].includes(item.status)).length;
      const item = { id: `r-${Date.now()}`, activityId: id, attendeeId: user().id,
        attendeeName: user().name, status: count >= activity.capacity ? "waitlisted" : "registered",
        waitlistPosition: count >= activity.capacity ? 1 : null };
      registrations[`${user().id}:${id}`] = item;
      return item;
    },
    async cancel(id) {
      const item = currentRegistration(id);
      if (!item) throw new Error("尚未报名");
      item.status = "cancelled";
      return item;
    },
    async roster(id) {
      const activity = activities.find((item) => item.id === id);
      if (activity.organizerId !== user().id) throw new Error("只能管理自己创建的活动");
      const entries = Object.values(registrations).filter((item) => item.activityId === id)
        .filter((item) => item.status !== "cancelled")
        .map((item, index) => ({ registrationId: item.id, attendeeId: item.attendeeId,
          attendeeName: item.attendeeName || "林一", status: item.status,
          queuePosition: item.status === "waitlisted" ? index + 1 : 0 }));
      return { activityId: id, title: activity.title, entries };
    },
    async checkIn(id, registrationId) {
      const item = Object.values(registrations).find((entry) => entry.id === registrationId
        && entry.activityId === id);
      if (!item || item.status !== "registered") throw new Error("只能签到已报名参与者");
      item.status = "checked_in";
      return item;
    },
    async credential(id) {
      const item = currentRegistration(id);
      if (!item || item.status !== "registered") throw new Error("只有已报名且未签到的参与者可以领取签到凭证");
      const previousCode = credentialByRegistration[item.id];
      if (previousCode) delete credentialCodes[previousCode];
      const code = `CAMPUS-${Math.random().toString(36).slice(2, 12).toUpperCase()}-${Date.now().toString(36).toUpperCase()}`;
      credentialCodes[code] = item.id;
      credentialByRegistration[item.id] = code;
      return { activityId: id, code };
    },
    async verifyCredential(id, code) {
      const registrationId = credentialCodes[code.trim()];
      if (!registrationId) throw new Error("签到凭证无效");
      return this.checkIn(id, registrationId);
    }
  };
}
