import { api } from "../api/client.js?v=20260710-activity-review-ui-v1";
import { state } from "../state.js";
import { loadAdminData, loadFeed, loadPersonalPosts } from "../loaders.js?v=20260710-activity-review-ui-v1";
import { renderPersonalPostManager } from "./renderers.js?v=20260710-activity-review-ui-v1";

export async function bindPersonalPostEvents(event) {
  const openManager = event.target.closest("#managePersonalPosts");
  if (openManager) {
    state.personalPostManagerOpen = !state.personalPostManagerOpen;
    state.editingPersonalPostId = null;
    if (state.personalPostManagerOpen) {
      await loadPersonalPosts();
    } else {
      renderPersonalPostManager();
    }
    return true;
  }

  const editPost = event.target.closest("[data-edit-personal-post]");
  if (editPost) {
    state.editingPersonalPostId = Number(editPost.dataset.editPersonalPost);
    renderPersonalPostManager();
    return true;
  }

  const cancelEdit = event.target.closest("[data-cancel-personal-post-edit]");
  if (cancelEdit) {
    state.editingPersonalPostId = null;
    renderPersonalPostManager();
    return true;
  }

  const savePost = event.target.closest("[data-save-personal-post]");
  if (savePost) {
    const postId = Number(savePost.dataset.savePersonalPost);
    const editor = document.querySelector(`[data-personal-post-editor="${postId}"]`);
    const body = editor ? editor.value.trim() : "";
    if (!body) return true;
    await api.updatePersonalPost(postId, body);
    state.editingPersonalPostId = null;
    await Promise.all([loadFeed(), loadPersonalPosts(), loadAdminData()]);
    return true;
  }

  const deletePost = event.target.closest("[data-delete-personal-post]");
  if (deletePost) {
    await api.deletePersonalPost(Number(deletePost.dataset.deletePersonalPost));
    await Promise.all([loadFeed(), loadPersonalPosts(), loadAdminData()]);
    return true;
  }

  return false;
}
