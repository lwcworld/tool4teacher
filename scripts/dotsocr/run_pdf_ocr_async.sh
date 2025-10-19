#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_PDF="${SCRIPT_DIR}/../../dataset/downloads/suneung/수학영역_문제지.pdf"

PDF_PATH="${DEFAULT_PDF}"
if (( $# > 0 )); then
  PDF_PATH="$1"
  shift
fi

python3 "${SCRIPT_DIR}/pdf_ocr_async.py" --pdf "${PDF_PATH}" "$@"
