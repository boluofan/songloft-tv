#!/bin/bash

# Songloft TV 版本发布脚本
# 用法：./scripts/bump-version.sh [release|major|minor|patch] [--dry-run]
# 示例：
#   ./scripts/bump-version.sh release          # 2.0.0-alpha.2 -> 2.0.0（去掉预发布后缀）
#   ./scripts/bump-version.sh patch             # 2.0.0 -> 2.0.1
#   ./scripts/bump-version.sh minor --dry-run   # 仅预览，不修改
#
# 流程：
#   1. 更新 app/build.gradle.kts 中的 versionName / versionCode
#   2. git commit + tag + push（push tag 后由 .github/workflows/build-and-release.yml
#      完成 APK 构建、GitHub Release，以及 CHANGELOG 生成）
#
# 最后一行 stdout 输出新版本号（带 v 前缀），方便链式调用。

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ============================================================
# 参数解析
# ============================================================
BUMP_TYPE=""
DRY_RUN=false

for arg in "$@"; do
    case "$arg" in
        release|major|minor|patch)
            BUMP_TYPE="$arg"
            ;;
        --dry-run)
            DRY_RUN=true
            ;;
        -h|--help)
            echo "Songloft TV 版本发布工具"
            echo ""
            echo "用法:"
            echo "  $0 [release|major|minor|patch] [--dry-run]"
            echo ""
            echo "参数:"
            echo "  release  - 正式发布（去掉预发布后缀：2.0.0-alpha.2 -> 2.0.0）"
            echo "  major    - 主版本号升级 (1.0.0 -> 2.0.0)"
            echo "  minor    - 次版本号升级 (1.0.0 -> 1.1.0)"
            echo "  patch    - 补丁版本号升级 (1.0.0 -> 1.0.1，默认)"
            echo "  --dry-run - 仅打印将要执行的操作，不实际修改文件"
            echo ""
            echo "示例:"
            echo "  $0 release             # 2.0.0-alpha.2 -> 2.0.0"
            echo "  $0 patch               # 2.0.0 -> 2.0.1"
            echo "  $0 minor --dry-run     # 仅预览，不修改"
            exit 0
            ;;
        *)
            echo -e "${RED}错误：未知参数 '$arg'${NC}" >&2
            echo "用法：$0 [release|major|minor|patch] [--dry-run]" >&2
            exit 1
            ;;
    esac
done

# 默认 patch
BUMP_TYPE="${BUMP_TYPE:-patch}"

# ============================================================
# 工具函数
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
GRADLE_FILE="$PROJECT_DIR/app/build.gradle.kts"

log_info() {
    echo -e "${BLUE}$1${NC}" >&2
}

log_ok() {
    echo -e "${GREEN}✓${NC} $1" >&2
}

log_warn() {
    echo -e "${YELLOW}警告：$1${NC}" >&2
}

log_err() {
    echo -e "${RED}错误：$1${NC}" >&2
}

# 获取当前版本号（从 build.gradle.kts 的 versionName）
get_current_version() {
    if [ ! -f "$GRADLE_FILE" ]; then
        log_err "找不到 $GRADLE_FILE"
        exit 1
    fi

    local version
    version=$(grep 'versionName = ' "$GRADLE_FILE" | head -1 | sed 's/.*versionName = "\(.*\)".*/\1/')

    if [ -z "$version" ]; then
        log_err "build.gradle.kts 中找不到 versionName 字段"
        exit 1
    fi

    echo "$version"
}

# 获取当前 versionCode
get_current_version_code() {
    grep 'versionCode = ' "$GRADLE_FILE" | head -1 | sed 's/.*versionCode = \([0-9]*\).*/\1/'
}

# 提取基础版本号（去掉预发布后缀：2.0.0-alpha.2 -> 2.0.0）
strip_prerelease() {
    echo "$1" | cut -d'-' -f1
}

# 升级版本号
bump_version() {
    local version="$1"
    local bump_type="$2"

    # 先去掉预发布后缀
    version=$(strip_prerelease "$version")

    local major minor patch
    major=$(echo "$version" | cut -d. -f1)
    minor=$(echo "$version" | cut -d. -f2)
    patch=$(echo "$version" | cut -d. -f3)

    if ! [[ "$major" =~ ^[0-9]+$ ]] || ! [[ "$minor" =~ ^[0-9]+$ ]] || ! [[ "$patch" =~ ^[0-9]+$ ]]; then
        log_err "无效的版本号格式 '$version'"
        exit 1
    fi

    case "$bump_type" in
        release)
            # 直接使用基础版本号（去掉预发布后缀即可）
            ;;
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
    esac

    echo "${major}.${minor}.${patch}"
}

# 更新 build.gradle.kts（versionName 更新为新版本，versionCode 自增 1）
update_gradle() {
    local new_version=$1
    local current_code new_code
    current_code=$(get_current_version_code)
    new_code=$((current_code + 1))

    sed -i "s/versionName = \".*\"/versionName = \"${new_version}\"/" "$GRADLE_FILE"
    sed -i "s/versionCode = [0-9]*/versionCode = ${new_code}/" "$GRADLE_FILE"

    log_ok "build.gradle.kts 已更新：versionName=${new_version} versionCode=${new_code}"
}

# ============================================================
# 主流程
# ============================================================
main() {
    log_info "=== Songloft TV 版本发布工具 ==="
    if [ "$DRY_RUN" = true ]; then
        log_warn "DRY-RUN 模式：不会实际修改任何文件"
    fi
    echo "" >&2

    # 检查 git 仓库
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        log_err "当前目录不是 git 仓库"
        exit 1
    fi

    # 检查 build.gradle.kts
    if [ ! -f "$GRADLE_FILE" ]; then
        log_err "未找到 $GRADLE_FILE，请在项目根目录运行此脚本"
        exit 1
    fi

    # 获取当前版本
    local current_version
    current_version=$(get_current_version)
    if [ -z "$current_version" ]; then
        log_err "无法从 build.gradle.kts 读取版本号"
        exit 1
    fi

    # 计算新版本
    local new_version
    new_version=$(bump_version "$current_version" "$BUMP_TYPE")

    log_info "当前版本: ${current_version}"
    log_info "新版本:   ${new_version}"
    log_info "升级类型: ${BUMP_TYPE}"
    echo "" >&2

    # 检查版本号是否有变化
    if [ "$current_version" = "$new_version" ]; then
        log_warn "版本号未变化（当前已是 ${new_version}），无需发布"
        exit 0
    fi

    if [ "$DRY_RUN" = false ]; then
        read -p "确认发布版本 ${new_version}？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${RED}已取消${NC}"
            exit 1
        fi
    fi

    # CI 环境：自动配置 git user
    if [ -z "$(git config user.email 2>/dev/null)" ]; then
        log_warn "git user.email 未设置，自动配置为 CI 用户"
        if [ "$DRY_RUN" = false ]; then
            git config user.email 'ci@songloft'
            git config user.name 'Songloft CI'
        fi
    fi

    # 1. 更新 build.gradle.kts
    log_info "[1/4] 更新 build.gradle.kts 中的版本号..."
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[dry-run]${NC} build.gradle.kts: versionName ${current_version} -> ${new_version}, versionCode +1" >&2
    else
        update_gradle "$new_version"
    fi
    log_ok "build.gradle.kts 已更新"

    # 2. 提交更改
    log_info "[2/4] 提交更改到 git..."
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[dry-run]${NC} git add app/build.gradle.kts" >&2
        echo -e "${YELLOW}[dry-run]${NC} git commit -m 'chore: release version ${new_version}'" >&2
    else
        git add "$GRADLE_FILE"
        if ! git diff-index --quiet --cached HEAD --; then
            git commit -m "chore: release version ${new_version}"
            log_ok "更改已提交"
        else
            log_warn "没有检测到需要提交的更改"
        fi
    fi

    # 3. 创建 git tag
    log_info "[3/4] 创建 git tag..."
    local tag_name="v${new_version}"
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[dry-run]${NC} git tag -a '${tag_name}' -m 'Release version ${new_version}'" >&2
    else
        if git rev-parse "$tag_name" >/dev/null 2>&1; then
            log_warn "Git 标签 '${tag_name}' 已存在"
            read -p "是否覆盖现有标签？(y/N) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                git tag -d "$tag_name" >/dev/null 2>&1 || true
                git push origin ":refs/tags/$tag_name" >/dev/null 2>&1 || true
                log_ok "已删除旧标签"
            else
                echo -e "${RED}已取消${NC}"
                exit 1
            fi
        fi
        git tag -a "$tag_name" -m "Release version ${new_version}"
    fi
    log_ok "Git 标签 ${tag_name} 已创建"

    # 4. 推送
    log_info "[4/4] 推送更改和 tag..."
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[dry-run]${NC} git push --follow-tags" >&2
    else
        git push --follow-tags
    fi
    log_ok "已推送到远程仓库"

    echo "" >&2
    local repo_url
    repo_url=$(git remote get-url origin | sed -e 's/^git@github.com:/https:\/\/github.com\//' -e 's/\.git$//')
    log_info "Release URL: ${repo_url}/releases/tag/${tag_name}"

    # 最后一行 stdout 输出新版本号（带 v 前缀），方便链式调用
    echo "v${new_version}"
}

main
