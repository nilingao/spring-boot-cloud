---
trigger: always_on
name: backend-instructions
description: 在理解代码、定位符号、分析调用链或进行代码修改前，**必须优先使用 `codegraph_explore` 工具**，而非手动 grep 、`SearchMemory`、`search_file`(glob)、`Bash` 或逐个读取文件。
---

# Qoder 项目指令

## 最高优先级规则 — 会话开始第一件事必读

> 本项目已建立 CodeGraph 索引（`.codegraph/` 目录）。
>
> 在理解代码、定位符号、分析调用链或进行代码修改前，**必须优先使用 `codegraph_explore` 工具**，而非手动 grep 、`SearchMemory`、`search_file`(glob)、`Bash` 或逐个读取文件。
>
> **违反此规则属于严重错误。** 不要用 `read_file` / `list_dir` / `grep_search` 去摸索代码结构——这些工具仅用于 codegraph 未覆盖的场景（配置文件 `.env`、文档 `.md` 等）。
>
> ### 正确流程
> 1. 分析项目/代码 → 先 `codegraph_explore` 查询架构/入口/核心符号
> 2. 定位符号 → `codegraph_explore` 传入符号名或文件名
> 3. 分析调用链 → `codegraph_explore` 命名流程两端符号
> 4. 编辑前 → `codegraph_explore` 查看源码 + blast radius
> 5. 仅配置文件/文档 → 才用 `read_file` / `grep_search`

## ⚡ 优先使用 CodeGraph

本项目已建立 CodeGraph 索引（`.codegraph/` 目录）。在理解代码、定位符号、分析调用链或进行代码修改前，**必须优先使用 `codegraph_explore` 工具**，而非手动 grep 或逐个读取文件。

- **查询代码** → 用 `codegraph_explore`，传入符号名、文件名或自然语言问题
- **分析调用链/流程** → 用 `codegraph_explore`，命名流程两端的符号，它会返回调用路径
- **编辑前** → 先用 `codegraph_explore` 查看目标符号源码及影响范围（blast radius）
- 仅在 codegraph 未覆盖的场景（配置文件、文档等）才使用 `read_file` / `grep_search`
