const accounts = [
  { id: "u-1001", name: "林一", role: "学生账号", phone: "13800000001" },
  { id: "u-2001", name: "陈老师", role: "教师账号", phone: "13800000002" },
  { id: "u-2003", name: "教务管理员", role: "管理员", phone: "13800000004" }
];

export function createMockAuthApi() {
  const codes = new Map();
  return {
    async createCode(phone) {
      const code = "123456";
      codes.set(phone, code);
      return { phone, code };
    },
    async login(phone, code) {
      if (codes.get(phone) !== code) throw new Error("验证码不正确");
      const user = accounts.find((account) => account.phone === phone) || {
        ...accounts[0],
        phone
      };
      return { token: `mock-jwt-token-${user.id}`, user };
    },
    async demoLogin(userId) {
      const user = accounts.find((account) => account.id === userId) || accounts[0];
      return { token: `mock-jwt-token-${user.id}`, user };
    },
    async logout() {}
  };
}
