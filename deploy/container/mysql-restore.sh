#!/bin/sh
set -eu

database=$1
archive=$2

case "$database" in
  ''|[0-9]*|*[!A-Za-z0-9_]*)
    echo "invalid database name" >&2
    exit 64
    ;;
esac

export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
mysql --user=root --execute="DROP DATABASE IF EXISTS $database; CREATE DATABASE $database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
gzip -dc "$archive" | mysql --user=root "$database"
