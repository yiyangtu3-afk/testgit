import { state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260715-notification-actions-v1";
import { escapeHtml } from "../utils/format.js?v=20260715-notification-actions-v1";

export function renderFeed() {
  const feedList = $("#feedList");
  if (state.personalPostManagerOpen) {
    feedList.hidden = true;
    feedList.innerHTML = "";
    return;
  }
  feedList.hidden = false;
  const notice = feedNoticeMarkup();
  const feedRows = state.posts
    .filter((post) => post.moderationStatus === "approved")
    .map(
      (post) => {
        const expanded = state.expandedPostId === post.id;
        const liked = Boolean(post.likedByCurrentUser);
        const comments = state.postComments[post.id] || [];
        const commentRows = comments.length
          ? comments
              .map(
                (comment) => `
                  <article class="comment-row">
                    <span class="avatar">${escapeHtml((comment.author || "?").slice(0, 1))}</span>
                    <div>
                      <h4>${escapeHtml(comment.author)}</h4>
                      <p>${escapeHtml(comment.body)}</p>
                      <time>${escapeHtml(comment.time)}</time>
                    </div>
                  </article>
                `
              )
              .join("")
          : `<p class="empty-copy">还没有评论。</p>`;
        return `
        <article class="feed-card${state.notificationPostFocusId === post.id ? " feed-card--notification-target" : ""}" data-post-id="${escapeHtml(post.id)}">
          <div class="feed-head">
            <span class="avatar">${escapeHtml((post.author || "?").slice(0, 1))}</span>
            <div>
              <h3>${escapeHtml(post.author)}</h3>
              <p>${escapeHtml(post.visibility)}${post.moderationStatus === "pending" ? " / 待审核" : ""}</p>
            </div>
          </div>
          <p>${escapeHtml(post.body)}</p>
          <div class="feed-actions">
            <button class="small-button${liked ? " is-liked" : ""}" data-like="${escapeHtml(post.id)}" type="button" aria-pressed="${liked}">${liked ? "已赞" : "赞"} ${escapeHtml(post.likes)}</button>
            <button class="small-button" data-comments="${escapeHtml(post.id)}" type="button">评论 ${escapeHtml(post.comments)}</button>
          </div>
          ${
            expanded
              ? `
                <section class="comment-panel">
                  <div class="comment-list">${commentRows}</div>
                  <form class="comment-form" data-comment-form="${escapeHtml(post.id)}">
                    <input type="text" name="comment" placeholder="写一条评论" />
                    <button class="primary-button" type="submit">发送</button>
                  </form>
                </section>
              `
              : ""
          }
        </article>
      `;
      }
    )
    .join("");
  feedList.innerHTML = `
    ${notice}
    ${feedRows || `<p class="empty-copy">暂无已通过的校园动态。</p>`}
  `;
}

export function renderPersonalPostManager() {
  const panel = $("#personalPostPanel");
  if (!state.personalPostManagerOpen) {
    panel.hidden = true;
    panel.innerHTML = "";
    $("#feedList").hidden = false;
    renderFeed();
    return;
  }
  $("#feedList").hidden = true;
  const rows = state.personalPosts.length
    ? state.personalPosts.map(renderPersonalPostRow).join("")
    : `<p class="empty-copy">你还没有发布过动态。</p>`;
  panel.hidden = false;
  panel.innerHTML = `
    <div class="personal-post-head">
      <div>
        <p class="section-kicker">Personal Moments</p>
        <h3>我的动态</h3>
      </div>
      <strong>${state.personalPosts.length}</strong>
    </div>
    ${feedNoticeMarkup()}
    <div class="review-state-guide">
      <span>待审核：提交后进入管理员审核，通过前仅自己可见。</span>
      <span>已发布：内容已通过审核，进入公共动态流。</span>
      <span>已拒绝：按原因修改后可重新提交。</span>
    </div>
    <div class="personal-post-list">${rows}</div>
  `;
}

function renderPersonalPostRow(post) {
  const editing = state.editingPersonalPostId === post.id;
  const status = personalPostStatus(post.moderationStatus);
  return `
    <article class="personal-post-row">
      <div>
        <span class="moderation-type personal-post-status personal-post-status--${status.key}">${status.label}</span>
        <small>${escapeHtml(post.visibility)} / ${escapeHtml(post.likes)} 赞 / ${escapeHtml(post.comments)} 评论</small>
      </div>
      <div class="personal-post-review-note personal-post-review-note--${status.key}">
        <strong>${status.heading}</strong>
        <span>${escapeHtml(personalPostReviewCopy(post, status))}</span>
      </div>
      ${
        editing
          ? `<textarea data-personal-post-editor="${escapeHtml(post.id)}">${escapeHtml(post.body)}</textarea>`
          : `<p>${escapeHtml(post.body)}</p>`
      }
      <div class="personal-post-actions">
        ${
          editing
            ? `
              <button class="small-button" data-save-personal-post="${escapeHtml(post.id)}" type="button">保存</button>
              <button class="small-button" data-cancel-personal-post-edit type="button">取消</button>
            `
            : `
              <button class="small-button" data-edit-personal-post="${escapeHtml(post.id)}" type="button">编辑</button>
              <button class="small-button" data-delete-personal-post="${escapeHtml(post.id)}" type="button">删除</button>
            `
        }
      </div>
    </article>
  `;
}

function feedNoticeMarkup() {
  if (!state.feedNotice) {
    return "";
  }
  return `
    <div class="feed-feedback feed-feedback--${state.feedNotice.kind}" role="status">
      ${escapeHtml(state.feedNotice.message)}
    </div>
  `;
}

function personalPostReviewCopy(post, status) {
  const reason = post.moderationReason || status.defaultReason;
  if (status.key === "pending") {
    return `审核原因：${reason}。通过前不会出现在公共动态流。`;
  }
  if (status.key === "rejected") {
    return `拒绝原因：${reason}。你可以编辑后重新提交审核。`;
  }
  return "审核已通过，其他用户可以在校园动态中看到这条内容。";
}

function personalPostStatus(moderationStatus) {
  const labels = {
    approved: { key: "approved", label: "已发布", heading: "审核通过", defaultReason: "内容符合校园动态规范" },
    pending: { key: "pending", label: "待审核", heading: "等待管理员审核", defaultReason: "内容待审核" },
    rejected: { key: "rejected", label: "已拒绝", heading: "审核未通过", defaultReason: "内容不符合校园动态规范" }
  };
  return labels[moderationStatus] || {
    key: "unknown",
    label: "未知状态",
    heading: "状态待确认",
    defaultReason: "请稍后刷新查看"
  };
}
