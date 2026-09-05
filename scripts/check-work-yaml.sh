#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
scripts/check-yaml-schema.sh
exec build/schema-check-venv/bin/python -B scripts/check-work-yaml.py
