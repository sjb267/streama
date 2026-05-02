<template>
  <div class="dashboard-view">
    <section class="metrics">
      <article class="metric-card">
        <p class="metric-label">今日处理稿件</p>
        <p class="metric-value">12,480</p>
        <p class="metric-footnote">较昨日提升 1.8%</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">累计播放量</p>
        <p class="metric-value">58,231</p>
        <p class="metric-footnote">当前在线稿件 128</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">待审核稿件</p>
        <p class="metric-value">426</p>
        <p class="metric-footnote">其中高风险 56 条</p>
      </article>
      <article class="metric-card">
        <p class="metric-label">异常任务数</p>
        <p class="metric-value">7</p>
        <p class="metric-footnote">需要人工关注 2 条</p>
      </article>
    </section>

    <section class="workbench">
      <article class="panel">
        <h3>工作概览</h3>
        <p>这里可以展示后台核心数据与审核进度，帮助管理员快速了解当前内容处理状态。</p>
      </article>
      <article class="panel">
        <h3>待办提醒</h3>
        <p>可扩展接入高风险稿件、失败任务、分类维护等提醒内容，方便集中处理重点事项。</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border: 1px solid #dbe4fa;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff, #f7faff);
}

.metric-label {
  margin: 0;
  font-size: 13px;
  color: #7283a6;
}

.metric-value {
  margin: 10px 0 8px;
  font-size: 30px;
  line-height: 1;
  color: #20304f;
  font-weight: 700;
}

.metric-footnote {
  margin: 0;
  color: #5a6f98;
  font-size: 12px;
}

.workbench {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.panel {
  border: 1px solid #dbe4fa;
  border-radius: 14px;
  padding: 16px;
  background: #ffffff;
}

.panel h3 {
  margin: 0;
  color: #223355;
}

.panel p {
  margin: 10px 0 0;
  font-size: 14px;
  color: #66799f;
  line-height: 1.7;
}

@media (max-width: 1000px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .metrics,
  .workbench {
    grid-template-columns: 1fr;
  }
}
</style>
