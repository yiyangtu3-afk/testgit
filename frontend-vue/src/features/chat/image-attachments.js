export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

const IMAGE_TYPES = new Set([
  "image/png",
  "image/jpeg",
  "image/webp",
  "image/gif"
]);

export function isImageAttachment(attachment) {
  return IMAGE_TYPES.has(attachment?.type);
}

export function isPreviewableImage(attachment) {
  return isImageAttachment(attachment) && Boolean(attachment.hasContent || attachment.dataUrl);
}

export function formatFileSize(size) {
  if (size < 1024) return `${size} B`;
  return `${Math.ceil(size / 1024)} KB`;
}

export async function createImageAttachments(files) {
  const selected = [...files];
  const invalid = selected.find((file) => !IMAGE_TYPES.has(file.type));
  if (invalid) {
    throw new Error("仅支持 PNG、JPEG、WebP 或 GIF 图片");
  }
  const oversized = selected.find((file) => file.size > MAX_IMAGE_BYTES);
  if (oversized) {
    throw new Error("单张图片不能超过 5 MB");
  }
  return Promise.all(selected.map(async (file) => ({
    id: crypto.randomUUID(),
    name: file.name,
    size: file.size,
    type: file.type,
    kind: "image",
    dataUrl: await readDataUrl(file)
  })));
}

function readDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("图片读取失败"));
    reader.readAsDataURL(file);
  });
}
