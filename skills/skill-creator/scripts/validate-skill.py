#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Claude Code Skill Validator

验证 skill 目录结构和 SKILL.md 文件是否符合最佳实践。
"""

import os
import re
import sys
import yaml

# 设置控制台编码为 UTF-8（Windows 兼容）
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
from pathlib import Path
from typing import List, Tuple


class SkillValidator:
    """Skill 验证器"""

    def __init__(self, skill_path: str):
        self.skill_path = Path(skill_path)
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.passed_checks: List[str] = []

    def validate(self) -> bool:
        """执行所有验证检查"""
        print(f"验证 skill: {self.skill_path}")
        print("=" * 50)

        self._check_directory_exists()
        self._check_skill_md_exists()
        self._check_yaml_frontmatter()
        self._check_name_format()
        self._check_description_length()
        self._check_references()
        self._check_scripts()
        self._check_path_formats()

        self._print_results()
        return len(self.errors) == 0

    def _check_directory_exists(self):
        """检查目录是否存在"""
        if not self.skill_path.exists():
            self.errors.append(f"目录不存在: {self.skill_path}")
        else:
            self.passed_checks.append("✓ 目录存在")

    def _check_skill_md_exists(self):
        """检查 SKILL.md 文件是否存在"""
        skill_md = self.skill_path / "SKILL.md"
        if not skill_md.exists():
            # 也检查 skill.md (小写)
            skill_md_lower = self.skill_path / "skill.md"
            if not skill_md_lower.exists():
                self.errors.append("SKILL.md 文件不存在")
                return
            self.warnings.append("文件名为 skill.md，建议使用 SKILL.md (大写)")
        else:
            self.passed_checks.append("✓ SKILL.md 存在")

    def _check_yaml_frontmatter(self):
        """检查 YAML frontmatter 是否有效"""
        skill_md = self.skill_path / "SKILL.md"
        if not skill_md.exists():
            skill_md = self.skill_path / "skill.md"
        if not skill_md.exists():
            return

        content = skill_md.read_text(encoding='utf-8')

        # 检查是否以 --- 开头
        if not content.startswith('---'):
            self.errors.append("缺少 YAML frontmatter (必须以 --- 开头)")
            return

        # 提取 frontmatter
        try:
            frontmatter_match = re.match(r'^---\n(.*?)\n---', content, re.DOTALL)
            if not frontmatter_match:
                self.errors.append("YAML frontmatter 格式无效 (必须以 ---\\n--- 包裹)")
                return

            frontmatter_text = frontmatter_match.group(1)
            frontmatter = yaml.safe_load(frontmatter_text)

            if not isinstance(frontmatter, dict):
                self.errors.append("YAML frontmatter 必须是键值对格式")
                return

            self.frontmatter = frontmatter
            self.passed_checks.append("✓ YAML frontmatter 格式有效")

            # 检查必需字段
            if 'name' not in frontmatter:
                self.errors.append("YAML frontmatter 缺少 'name' 字段")
            else:
                self.passed_checks.append(f"✓ name: {frontmatter['name']}")

            if 'description' not in frontmatter:
                self.errors.append("YAML frontmatter 缺少 'description' 字段")
            else:
                self.passed_checks.append(f"✓ description: {frontmatter['description'][:50]}...")

        except yaml.YAMLError as e:
            self.errors.append(f"YAML frontmatter 解析失败: {e}")
        except Exception as e:
            self.errors.append(f"解析 frontmatter 时出错: {e}")

    def _check_name_format(self):
        """检查 name 字段格式"""
        if not hasattr(self, 'frontmatter') or 'name' not in self.frontmatter:
            return

        name = self.frontmatter['name']

        # 检查长度
        if len(name) > 64:
            self.errors.append(f"name 长度超过 64 字符: {len(name)}")
        else:
            self.passed_checks.append(f"✓ name 长度符合要求 ({len(name)}/64)")

        # 检查格式：小写字母、数字、连字符
        if not re.match(r'^[a-z0-9-]+$', name):
            self.errors.append(f"name 格式无效: '{name}' (只能包含小写字母、数字和连字符)")
        else:
            self.passed_checks.append("✓ name 格式正确")

        # 检查是否以连字符开头或结尾
        if name.startswith('-') or name.endswith('-'):
            self.warnings.append("name 不应以连字符开头或结尾")

        # 检查是否有连续连字符
        if '--' in name:
            self.warnings.append("name 不应包含连续连字符")

    def _check_description_length(self):
        """检查 description 字段长度"""
        if not hasattr(self, 'frontmatter') or 'description' not in self.frontmatter:
            return

        description = self.frontmatter['description']

        if len(description) > 1024:
            self.errors.append(f"description 长度超过 1024 字符: {len(description)}")
        else:
            self.passed_checks.append(f"✓ description 长度符合要求 ({len(description)}/1024)")

        # 检查是否说明功能和时机
        if len(description) < 20:
            self.warnings.append("description 过短，应详细说明功能和使用时机")

    def _check_references(self):
        """检查 references 目录和引用的文件"""
        skill_md = self.skill_path / "SKILL.md"
        if not skill_md.exists():
            skill_md = self.skill_path / "skill.md"
        if not skill_md.exists():
            return

        content = skill_md.read_text(encoding='utf-8')

        # 查找所有 references/ 链接
        ref_links = re.findall(r'\[([^\]]+)\]\(references/([^\)]+)\)', content)

        if ref_links:
            refs_dir = self.skill_path / "references"
            if not refs_dir.exists():
                self.errors.append(f"SKILL.md 引用了 references/ 但目录不存在")
            else:
                for link_text, file_path in ref_links:
                    ref_file = refs_dir / file_path
                    if not ref_file.exists():
                        self.errors.append(f"引用的文件不存在: references/{file_path}")
                    else:
                        self.passed_checks.append(f"✓ 引用文件存在: references/{file_path}")

    def _check_scripts(self):
        """检查 scripts 目录中的脚本"""
        scripts_dir = self.skill_path / "scripts"
        if scripts_dir.exists():
            scripts = list(scripts_dir.glob("*.py"))
            for script in scripts:
                self.passed_checks.append(f"✓ 脚本文件: {script.name}")

    def _check_path_formats(self):
        """检查文件路径格式（应使用 Unix 风格）"""
        skill_md = self.skill_path / "SKILL.md"
        if not skill_md.exists():
            skill_md = self.skill_path / "skill.md"
        if not skill_md.exists():
            return

        content = skill_md.read_text(encoding='utf-8')

        # 检查 Windows 风格路径
        windows_paths = re.findall(r'[\w-]+\\\\[\w-]+', content)
        if windows_paths:
            self.warnings.append(f"发现 Windows 风格路径，建议使用 Unix 风格 (/): {windows_paths[0]}")
        else:
            self.passed_checks.append("✓ 路径格式正确 (Unix 风格)")

    def _print_results(self):
        """打印验证结果"""
        print("\n验证结果:")
        print("=" * 50)

        if self.passed_checks:
            print(f"\n通过 ({len(self.passed_checks)}):")
            for check in self.passed_checks:
                print(f"  {check}")

        if self.warnings:
            print(f"\n警告 ({len(self.warnings)}):")
            for warning in self.warnings:
                print(f"  ⚠ {warning}")

        if self.errors:
            print(f"\n错误 ({len(self.errors)}):")
            for error in self.errors:
                print(f"  ✗ {error}")

        print("\n" + "=" * 50)
        if len(self.errors) == 0:
            print("✓ 验证通过！")
            return 0
        else:
            print(f"✗ 验证失败：{len(self.errors)} 个错误需要修复")
            return 1


def main():
    if len(sys.argv) < 2:
        print("用法: python validate-skill.py <skill-path>")
        print("示例: python validate-skill.py ./my-skill")
        sys.exit(1)

    skill_path = sys.argv[1]
    validator = SkillValidator(skill_path)
    success = validator.validate()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
