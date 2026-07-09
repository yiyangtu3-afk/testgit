import { api } from "../api/client.js?v=20260708-user-moderation-scroll-v2";
import { state } from "../state.js";
import { $ } from "../utils/dom.js";
import { validPhone } from "../utils/format.js?v=20260708-user-moderation-scroll-v2";
import { setLoginBusy, setStatus } from "../ui/status.js";
import { enterWorkspace } from "./workspace.js?v=20260708-user-moderation-scroll-v2";

export async function sendLoginCode() {
  const phone = $("#phoneInput").value.trim();
  if (!validPhone(phone)) {
    setStatus("请输入 11 位中国大陆手机号。", true);
    return;
  }

  try {
    setLoginBusy(true);
    setStatus("正在获取验证码...");
    const response = await api.createCode(phone);
    state.verificationCode = response.code;
    $("#codeInput").value = response.code;
    setStatus(`验证码 ${response.code} 已填入，可直接登录。`);
  } catch (error) {
    setStatus(error.message || "验证码获取失败。", true);
  } finally {
    setLoginBusy(false);
  }
}

export async function loginWithCode() {
  const phone = $("#phoneInput").value.trim();
  let code = $("#codeInput").value.trim();
  if (!validPhone(phone)) {
    setStatus("请输入 11 位中国大陆手机号。", true);
    return;
  }

  try {
    setLoginBusy(true);
    if (!code) {
      setStatus("正在获取验证码...");
      const codeResponse = await api.createCode(phone);
      code = codeResponse.code;
      $("#codeInput").value = code;
    }
    setStatus("正在登录...");
    const response = await api.login(phone, code);
    await enterWorkspace(response);
  } catch (error) {
    setStatus(error.message || "登录失败，请检查验证码。", true);
  } finally {
    setLoginBusy(false);
  }
}

export async function enterDemoWorkspace() {
  try {
    const response = await api.loginAsDemo("u-1001");
    await enterWorkspace(response);
  } catch (error) {
    setStatus(error.message || "进入演示失败。", true);
  }
}
