---
trigger: always_on
---

# Qoder 项目指令

## ⚠️ 最高优先级 — 代码探索首选 CodeGraph

> **本项目已建立 CodeGraph 索引（`.codegraph/` 目录）。**
>
> 在理解代码、定位符号、分析调用链或修改代码前，**必须优先使用 `codegraph_explore`（MCP server: codegraph）**，而非 SearchCodebase / Grep / 逐个 Read。
>
> 调用参数：`query`（符号名 / 文件名 / 自然语言问题）+ `projectPath`（`F:\work\denajia\whst-server`）。本会话若未读过 schema，先读 `mcps/codegraph/tools/codegraph_explore.json`。

### 正确流程
1. 分析项目 / 架构 / 入口 → `codegraph_explore` 查询核心符号
2. 定位符号 → `codegraph_explore` 传入符号名或文件名
3. 分析调用链 / 流程 → `codegraph_explore` 命名流程两端符号，返回调用路径
4. 编辑前 → `codegraph_explore` 查看目标源码 + 影响面（blast radius）
5. 返回中已展示的源码视为已读，**不要重复 Read 这些文件**；每轮 explore 有预算（本项目约 2 次），超出后再用 Read 补特定行范围

### 优先使用 CodeGraph
本项目已建立 CodeGraph 索引（`.codegraph/` 目录）。在理解代码、定位符号、分析调用链或进行代码修改前，**必须优先使用 `codegraph_explore` 工具**，而非手动 grep 或逐个读取文件。

1. 查询代码 → 用 `codegraph_explore`，传入符号名、文件名或自然语言问题
2. 分析调用链/流程 → 用 `codegraph_explore`，命名流程两端的符号，它会返回调用路径
3. 编辑前 → 先用 `codegraph_explore` 查看目标符号源码及影响范围（blast radius）
4. 仅在 codegraph 未覆盖的场景（配置文件、文档等）才使用 `read_file` / `grep_search`
