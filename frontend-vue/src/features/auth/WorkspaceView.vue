<template>
  <section class="workspace-content">
    <div class="workspace-intro">
      <p class="eyebrow">MIGRATION CONTROL</p>
      <h2>先让每一次跳转都有清晰的边界。</h2>
      <p>这不是业务首页。它是新旧前端并行期间的应用壳，用来统一承载导航、身份和
        数据来源状态。</p>
    </div>

    <div class="migration-grid">
      <article class="migration-card is-complete"><span>01</span><h3>认证边界</h3><p>验证码登录、快速进入、令牌保存与注销。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>02</span><h3>应用壳</h3><p>统一导航、账号上下文和 API/Mock 状态提示。</p><strong>已完成</strong></article>
      <article class="migration-card"><span>03</span><h3>联系人与聊天</h3><p>下一切片继续保持好友关系与实时消息边界。</p><strong>下一项</strong></article>
    </div>

    <section class="baseline-note">
      <p class="eyebrow">FUNCTIONAL BASELINE</p>
      <p><strong>旧版仍是当前完整 Demo。</strong> 聊天、动态、活动、通知和管理员功能尚未
        迁移；在功能等价前，请继续从根目录静态入口演示这些流程。</p>
    </section>
  </section>
</template>
