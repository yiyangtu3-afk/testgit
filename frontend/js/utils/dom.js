export const $ = (selector) => document.querySelector(selector);
export const nowTime = () => new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
