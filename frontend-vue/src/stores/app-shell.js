import { ref } from "vue";
import { defineStore } from "pinia";

export const useAppShellStore = defineStore("app-shell", () => {
  const notice = ref({ message: "", tone: "info" });

  function setNotice(message, tone = "info") {
    notice.value = { message, tone };
  }

  return { notice, setNotice };
});
