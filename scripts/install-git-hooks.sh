#!/usr/bin/env bash
# ============================================================
# 安装 git 钩子（ADR-0033 分支方法论 · 本地兜底）
#
# 用法:  ./scripts/install-git-hooks.sh
# 效果:  git config core.hooksPath scripts/git-hooks
#         → 直推 main / dev 被 pre-push 钩子拦截
#
# 卸载:  git config --unset core.hooksPath
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/.."

chmod +x scripts/git-hooks/pre-push
git config core.hooksPath scripts/git-hooks

echo "✅ git 钩子已安装: core.hooksPath = $(git config core.hooksPath)"
echo "   验证: git push 直推 main / dev 将被拦截（ADR-0033）"
