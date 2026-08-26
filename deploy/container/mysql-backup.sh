#!/bin/sh
set -eu

destination=$1
umask 077

MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump \
  --user=root \
  --single-transaction \
  --quick \
  --routines \
  --events \
  --triggers \
  --hex-blob \
  --set-gtid-purged=OFF \
  "$MYSQL_DATABASE" | gzip -9 > "$destination"

test -s "$destination"
