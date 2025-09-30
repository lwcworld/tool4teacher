#!/usr/bin/env bash
# add-submodule.sh (improved)
# - Fixes quoted section bug
# - Detects .gitignore conflicts and adds --force automatically
# - Optional cleanup of rogue quoted sections

set -euo pipefail
die() { echo "❌ $*" >&2; exit 1; }

normalize_path() {
  local p="$1"
  while [[ "$p" == ./* ]]; do p="${p:2}"; done
  p="${p%/}"
  printf '%s' "$p"
}

prompt() {
  local msg="$1" default="${2:-}"
  local ans
  if [[ -n "$default" ]]; then
    read -rp "$msg [$default]: " ans
    printf '%s' "${ans:-$default}"
  else
    read -rp "$msg: " ans
    printf '%s' "$ans"
  fi
}

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "리포 루트에서 실행하세요."

GIT_URL="$(prompt '서브모듈 Git URL 입력')"
[[ -n "$GIT_URL" ]] || die "Git URL은 비어 있을 수 없습니다."

RAW_PATH="$(prompt '클론할 경로(path) 입력 (예: third_party/TurboRAG)')"
[[ -n "$RAW_PATH" ]] || die "경로는 비어 있을 수 없습니다."
SUB_PATH="$(normalize_path "$RAW_PATH")"

[[ "$SUB_PATH" != *\"* ]] || die "경로에 쌍따옴표(\")를 포함할 수 없습니다: $SUB_PATH"

IGNORE_MODE="$(prompt 'ignore 모드 선택 (dirty/all)' 'dirty')"
case "$IGNORE_MODE" in dirty|all) ;; *) die "IGNORE 모드는 dirty 또는 all 만 가능합니다." ;; esac

DEFAULT_MSG="Add submodule: ${SUB_PATH} (ignore ${IGNORE_MODE})"
COMMIT_MSG="$(prompt '커밋 메시지 입력' "$DEFAULT_MSG")"

echo
echo "▶ 준비 내용 확인"
echo "   URL   : $GIT_URL"
echo "   PATH  : $SUB_PATH"
echo "   IGNORE: $IGNORE_MODE"
echo "   COMMIT: $COMMIT_MSG"
read -rp "진행할까요? [Y/n]: " CONFIRM
CONFIRM="${CONFIRM:-Y}"
[[ "$CONFIRM" =~ ^[Yy]$ ]] || die "사용자 취소"

# --- main ------------------------------------------------------------------

# 0) 이 경로가 ignore 되는지 확인
FORCE=()
if git check-ignore -v "$SUB_PATH" >/dev/null 2>&1; then
  echo "ℹ️  '$SUB_PATH' 경로가 .gitignore 등에 의해 무시되고 있어요:"
  git check-ignore -v "$SUB_PATH" || true
  FORCE=(--force)
fi

# 1) add submodule (섹션명은 path로 고정; 필요 시 --force 자동)
git submodule add ${FORCE[@]+"${FORCE[@]}"} --name "$SUB_PATH" "$GIT_URL" "$SUB_PATH"

# 2) .gitmodules 에 ignore 추가 (따옴표 이스케이프 금지)
git config -f .gitmodules submodule."$SUB_PATH".ignore "$IGNORE_MODE"
git add .gitmodules

# 3) 로컬 .git/config에도 동일 설정
git config submodule."$SUB_PATH".ignore "$IGNORE_MODE"

# 4) 과거 실수로 생긴 '따옴표 포함 섹션명' 정리
BADSEC=submodule.\""$SUB_PATH"\"
if git config -f .gitmodules --name-only --get-regexp "^$BADSEC\\." >/dev/null 2>&1; then
  git config -f .gitmodules --remove-section "$BADSEC" || true
  git add .gitmodules
fi
if git config --name-only --get-regexp "^$BADSEC\\." >/dev/null 2>&1; then
  git config --remove-section "$BADSEC" || true
fi

# 5) 검증
git config -f .gitmodules --get "submodule.$SUB_PATH.ignore" >/dev/null \
  || die ".gitmodules 에 ignore 설정이 적용되지 않았습니다."
git config --get "submodule.$SUB_PATH.ignore" >/dev/null \
  || die ".git/config 에 ignore 설정이 적용되지 않았습니다."

# 6) 커밋
git commit -m "$COMMIT_MSG"

# 7) 결과 표시
echo
echo "✅ 완료! 현재 ignore 설정:"
git config -f .gitmodules --get-regexp '^submodule\..*\.ignore' || true
git config --get-regexp '^submodule\..*\.ignore' || true

echo
echo "참고) 다른 클론에서 반영하려면:"
echo "  git submodule init"
echo "  git submodule sync --recursive"
