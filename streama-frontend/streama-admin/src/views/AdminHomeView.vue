<template>
  <div class="home-view">
    <section class="welcome-card">
      <p class="welcome-brand">Streama 管理后台</p>
      <h2>欢迎使用控制台</h2>
      <p class="welcome-copy">在这里可以进入分类管理、稿件管理与审核流程，快速完成后台日常工作。</p>
    </section>

    <section class="quick-grid">
      <article class="quick-card">
        <h3>分类管理</h3>
        <p>维护一级与二级分类，配置图标、编码和名称。</p>
      </article>
      <article class="quick-card">
        <h3>稿件管理</h3>
        <p>查看待审核稿件，按文件查看 AI 审核结果和命中内容。</p>
      </article>
      <article class="quick-card">
        <h3>人工审核</h3>
        <p>结合视频播放内容与 AI 参考信息，完成最终通过或驳回。</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-card,
.quick-card {
  border: 1px solid #dbe4fa;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f7faff);
  box-shadow: 0 12px 28px rgba(55, 74, 131, 0.06);
}

.welcome-card {
  padding: 22px;
}

.welcome-brand {
  margin: 0;
  color: #4f6eff;
  font-size: 13px;
  font-weight: 600;
}

.welcome-card h2 {
  margin: 10px 0 0;
  color: #1f2a44;
  font-size: 28px;
}

.welcome-copy {
  margin: 10px 0 0;
  color: #6f7f9f;
  font-size: 14px;
  line-height: 1.7;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  padding: 18px;
}

.quick-card h3 {
  margin: 0;
  color: #233554;
  font-size: 18px;
}

.quick-card p {
  margin: 10px 0 0;
  color: #6d7ea2;
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
