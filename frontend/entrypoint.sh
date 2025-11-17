#!/bin/sh
set -e

# Replace the placeholder with the actual API URL
sed -i "s|REPLACE_ME|${VITE_API_URL:-/api/v1}|g" /usr/share/nginx/html/config.js

exec "$@"