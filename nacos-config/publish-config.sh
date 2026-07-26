#!/bin/sh
set -eu

server_url="${NACOS_SERVER_URL:-http://nacos:8848}"
group_name="${NACOS_GROUP:-CAMPUSLINK_DEV}"

publish() {
  data_id="$1"
  content_file="$2"
  curl --fail --silent --show-error --retry 12 --retry-delay 2 \
    --request POST "${server_url}/nacos/v3/admin/cs/config" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "groupName=${group_name}" \
    --data-urlencode "content@${content_file}" \
    --data-urlencode "type=yaml" >/dev/null
  echo "Published ${group_name}/${data_id}"
}

publish campuslink-api.yaml /config/campuslink-api.yaml
publish campuslink-activity-service.yaml /config/campuslink-activity-service.yaml
publish campuslink-notification-service.yaml /config/campuslink-notification-service.yaml
publish campuslink-gateway.yaml /config/campuslink-gateway.yaml
