#!/bin/sh
set -eu

find /restore -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
tar -xzf /tmp/uploads.tar.gz -C /restore
