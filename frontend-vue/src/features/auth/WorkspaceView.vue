<template>
  <section class="workspace-content">
    <div class="workspace-intro">
      <p class="eyebrow">MIGRATION CONTROL</p>
      <h2>先让每一次跳转都有清晰的边界。</h2>
      <p>Vue 已是默认演示入口。这里统一承载导航、身份和数据来源状态，也支持随时
        切换演示账号。</p>
    </div>

    <div class="migration-grid">
      <article class="migration-card is-complete"><span>01</span><h3>认证边界</h3><p>验证码登录、快速进入、令牌保存与注销。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>02</span><h3>应用壳</h3><p>统一导航、账号上下文和 API/Mock 状态提示。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>03</span><h3>联系人与聊天</h3><p>好友申请、会话、附件、未读和实时消息。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>04</span><h3>校园动态</h3><p>公开流、个人动态、点赞、评论和审核反馈。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>05</span><h3>活动闭环</h3><p>筛选、报名、候补、名单和签到。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>06</span><h3>通知中心</h3><p>未读状态、目标跳转与实时通知。</p><strong>已完成</strong></article>
      <article class="migration-card is-complete"><span>07</span><h3>管理员控制台</h3><p>指标、审核、审计和报表复用现有权限边界。</p><strong>已完成</strong></article>
    </div>

    <section class="baseline-note">
      <p class="eyebrow">FUNCTIONAL BASELINE</p>
      <p><strong>旧版仍保留为回退演示。</strong> 需要核对旧版行为时，可从登录页的
        **打开旧版演示** 入口进入。</p>
    </section>
  </section>
</template>
