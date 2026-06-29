#!/usr/bin/env bash
#
# 向本机 ~/.m2/toolchains.xml 声明 JDK 21 的安装位置。
# Maven Toolchains 据此让编译和测试运行在 JDK 21，无需修改默认 JAVA_HOME。
# 脚本只新增或更新 JDK 21 条目，保留文件中其他 JDK 版本的 toolchain。
# toolchains.xml 含本机绝对路径，属于机器本地配置，不进版本库；首次 clone 后运行一次即可。
#
# 用法：
#   backend/scripts/setup-toolchains.sh
#   JDK21_HOME=/path/to/jdk-21 backend/scripts/setup-toolchains.sh
#
set -euo pipefail

JDK_VERSION="21"
VENDOR="openjdk"

# 校验某个目录是否为可用的 JDK 21 Home。
is_jdk21() {
    local home="$1"
    [ -n "${home}" ] || return 1
    [ -x "${home}/bin/javac" ] || return 1
    local version
    version="$("${home}/bin/javac" -version 2>&1 | awk '{print $2}')"
    case "${version}" in
        21|21.*) return 0 ;;
        *) return 1 ;;
    esac
}

# 定位 JDK 21 的 Home 目录。JDK21_HOME 优先级最高，设置后若校验失败直接报错，保证覆盖语义。
find_jdk_home() {
    local candidate

    if [ -n "${JDK21_HOME:-}" ]; then
        if is_jdk21 "${JDK21_HOME}"; then
            printf '%s\n' "${JDK21_HOME}"
            return 0
        fi
        echo "JDK21_HOME=${JDK21_HOME} 不是有效的 JDK ${JDK_VERSION} 安装。" >&2
        return 2
    fi

    # macOS：已注册到系统的 JDK。java_home 在未匹配时可能回退到其他版本，因此仍需校验。
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        candidate="$(/usr/libexec/java_home -v "${JDK_VERSION}" 2>/dev/null || true)"
        if is_jdk21 "${candidate}"; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    fi

    # Homebrew keg-only 安装：Intel 与 Apple Silicon 前缀。
    for candidate in \
        /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
        /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home; do
        if is_jdk21 "${candidate}"; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    done

    return 1
}

set +e
JDK_HOME="$(find_jdk_home)"
find_status=$?
set -e

if [ ${find_status} -eq 2 ]; then
    # JDK21_HOME 已设置但无效，不回退到自动探测。
    exit 1
fi
if [ ${find_status} -ne 0 ] || [ -z "${JDK_HOME}" ]; then
    echo "未找到 JDK ${JDK_VERSION}。请先安装，或通过 JDK21_HOME 指定其 Home 目录。" >&2
    echo "Homebrew 安装示例：brew install openjdk@21" >&2
    exit 1
fi

TOOLCHAINS_FILE="${HOME}/.m2/toolchains.xml"
mkdir -p "${HOME}/.m2"

# 通过 python3 做 XML 安全合并：保留其他 JDK 版本条目，只 upsert JDK 21。
if ! command -v python3 >/dev/null 2>&1; then
    echo "需要 python3 来安全合并 ${TOOLCHAINS_FILE}，但未找到 python3。" >&2
    exit 1
fi

JDK_HOME="${JDK_HOME}" JDK_VERSION="${JDK_VERSION}" VENDOR="${VENDOR}" \
TOOLCHAINS_FILE="${TOOLCHAINS_FILE}" python3 <<'PY'
import os
import sys
import xml.etree.ElementTree as ET

NS = "http://maven.apache.org/TOOLCHAINS/1.1.0"
path = os.environ["TOOLCHAINS_FILE"]
jdk_home = os.environ["JDK_HOME"]
version = os.environ["JDK_VERSION"]
vendor = os.environ["VENDOR"]

ET.register_namespace("", NS)


def local_name(tag):
    # 去掉可能存在的命名空间前缀，按本地名比较，兼容有无命名空间两种文件。
    return tag.rsplit("}", 1)[-1]


def find_child(el, name):
    if el is None:
        return None
    for child in el:
        if local_name(child.tag) == name:
            return child
    return None


def find_children(el, name):
    return [child for child in el if local_name(child.tag) == name] if el is not None else []


def text(el):
    return el.text.strip() if el is not None and el.text else ""


def q(tag):
    return f"{{{NS}}}{tag}"


# 读取已有文件，保留其中已有条目。无法解析或根节点不是 toolchains 时备份后重建，不静默丢配置。
root = None
if os.path.exists(path):
    try:
        parsed = ET.parse(path).getroot()
        if local_name(parsed.tag) == "toolchains":
            root = parsed
        else:
            backup = path + ".bak"
            os.replace(path, backup)
            print(f"已有 toolchains.xml 根节点异常，已备份到 {backup}", file=sys.stderr)
    except ET.ParseError:
        backup = path + ".bak"
        os.replace(path, backup)
        print(f"已有 toolchains.xml 无法解析，已备份到 {backup}", file=sys.stderr)

if root is None:
    root = ET.Element(q("toolchains"))

# 移除同为 JDK 21 的旧条目，保留其他版本。按本地名匹配，兼容有无命名空间的已有文件。
for tc in find_children(root, "toolchain"):
    if text(find_child(tc, "type")) != "jdk":
        continue
    provides = find_child(tc, "provides")
    if provides is None:
        continue
    if text(find_child(provides, "version")) == version:
        root.remove(tc)

# 追加新的 JDK 21 条目，使用标准命名空间。
tc = ET.SubElement(root, q("toolchain"))
ET.SubElement(tc, q("type")).text = "jdk"
provides = ET.SubElement(tc, q("provides"))
ET.SubElement(provides, q("version")).text = version
ET.SubElement(provides, q("vendor")).text = vendor
config = ET.SubElement(tc, q("configuration"))
ET.SubElement(config, q("jdkHome")).text = jdk_home

try:
    ET.indent(root, space="  ")
except AttributeError:
    pass

tree = ET.ElementTree(root)
tmp = path + ".tmp"
tree.write(tmp, encoding="UTF-8", xml_declaration=True)
os.replace(tmp, path)
PY

echo "已更新 ${TOOLCHAINS_FILE}"
echo "JDK ${JDK_VERSION} Home：${JDK_HOME}"
echo "现在可在 backend/ 下运行：./mvnw test"
