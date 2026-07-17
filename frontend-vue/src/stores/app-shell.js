import { ref } from "vue";
import { defineStore } from "pinia";

export const useAppShellStore = defineStore("app-shell", () => {
  const notice = ref({
    message: "认证会话已就绪。选择导航以查看迁移进度。",
    tone: "success"
  });

  function setNotice(message, tone = "info") {
    notice.value = { message, tone };
  }

  return { notice, setNotice };
});
