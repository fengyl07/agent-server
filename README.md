# Alert Agent —— 运维告警智能研判助手

一个基于 **MCP + RAG + Tool Calling** 的运维告警 Agent，让运维同事通过自然语言直接查告警、查 SOP。

---

## 🎯 项目背景

运维平台每天产生大量告警，同事查告警、翻 KB/SOP 文档效率低。这个 Agent 能直接回答：

- 「今天有多少告警？」
- 「查一下 test02 的未关闭告警」
- 「磁盘使用率过高怎么处理？」

**只读 Copilot**：基于工具返回的真实告警数据作答，不编造 ID/数量；知识类问题走 RAG 检索，回答可标注来源。

---

## 🧱 项目架构

分五层，各干一件事：

```
用户层     Cursor / Chat 接口 / Postman
   ↓
MCP 层     POST /alertagent/mcp  （JSON-RPC 2.0：tools/list、tools/call）
   ↓
LLM 层     DeepSeek Tool Calling，决定调告警 Tool 还是 search_knowledge
   ↓
Tool 层    4 个告警 Tool + 1 个 search_knowledge（共用 McpToolRegistry）
   ↓
知识层     MD 切块 → Qwen Embedding → Qdrant 向量检索
```

| 层级 | 说明 |
|------|------|
| **用户层** | Cursor、Chat 接口、Postman 等入口 |
| **MCP 层** | 暴露 `tools/list`、`tools/call`，供 AI 客户端标准接入 |
| **LLM 层** | `AlertLlmAgent` 多轮 Tool Calling 编排 |
| **Tool 层** | 告警 OpenAPI 封装 + 知识检索 |
| **知识层** | Phase 1c prompt 注入 + Phase 1d RAG 按需检索 |

---

## ✨ 核心设计

### 1. MCP + LLM 共用一套 Tool 注册表

`McpToolRegistry` 同时服务 MCP 的 `tools/list` 和 LLM 的 function calling。**新增工具只写一次**，Chat 与 Cursor 同步生效。

### 2. RAG 按需检索

用户问知识类问题才查向量库（`search_knowledge`），不把整本手册塞进 Prompt，降低 Token 消耗，知识库可扩展。

### 3. 多级降级

| 场景 | 行为 |
|------|------|
| RAG 失败 / 未启用 | 检索返回空，不影响告警查询 |
| LLM 失败 / 未启用 | 降级到关键词路由（Phase 1） |
| 工具执行异常 | 错误文本回传 LLM，不中断对话 |

---

## 🔧 工具清单

| 工具名 | 说明 |
|--------|------|
| `count_today_alerts` | 统计今天告警数量 |
| `query_alerts` | 查询告警列表（默认未关闭） |
| `get_alert_detail` | 查询单条告警详情 |
| `find_similar_alerts` | 查找相似告警 |
| `search_knowledge` | 检索运维知识库（RAG，`rag.enabled=true` 时可用） |

---

## 🚀 快速开始

### 环境要求

- JDK 8+、Maven
- Alert OpenAPI 可访问
- LLM：DeepSeek（或 OpenAI 兼容 Chat API）
- RAG（可选）：Qdrant（默认 `127.0.0.1:6333`）+ 阿里 Qwen Embedding

### 主要接口

| 接口 | 地址 |
|------|------|
| MCP | `POST /alertagent/mcp` |
| Chat | `POST /alertagent/frontapi/v1/agent/chat` |

### MCP 检索知识库示例

```bash
curl -s -X POST http://127.0.0.1:8091/alertagent/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "search_knowledge",
      "arguments": {
        "query": "磁盘使用率过高怎么处理",
        "topK": 3
      }
    }
  }'
```

### 关键配置（`rag.*`）

```yaml
rag:
  enabled: true
  embedding:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${你的DashScope密钥}
    model: text-embedding-v4
    dimension: 1024
  qdrant:
    host: 127.0.0.1
    port: 6333
    collection: alert_agent_kb
```

> 敏感项（API Key）请走 Apollo 或环境变量，勿提交到仓库。  
> 测试 yml 中避免 `rag.enabled: ${rag.enabled:false}` 这类自引用占位符，易触发 Spring 循环引用。

---

## 📦 技术栈

- **Java + Spring Boot**（优云脚手架）
- **DeepSeek** — Tool Calling / 对话编排
- **阿里 Qwen** — text-embedding-v4（DashScope compatible-mode）
- **Qdrant** — 向量数据库（HTTP REST，无额外 SDK）
- **MCP** — JSON-RPC 2.0 工具暴露
- **Alert OpenAPI** — 告警数据来源（只读）

---

## 🚧 踩坑记录

### 坑 1：Qdrant 在 CentOS 7 上 GLIBC 不兼容

CentOS 7.5（glibc 2.17），官方 gnu 版 Qdrant 需要 GLIBC 2.27。换 **musl 静态链接版** 解决；共用机建议绑 `127.0.0.1`，目录隔离部署（如 `/opt/qdrant-poc`）。

### 坑 2：DeepSeek 不支持 Embedding

曾尝试用 DeepSeek 同一 Key 做向量化，返回 404。改用 **阿里 Qwen text-embedding-v4**，curl 验证 1024 维后再接入 Java。

### 坑 3：Spring 配置占位符循环引用

`rag.enabled: ${rag.enabled:false}`、`chunk-size: ${rag.index.chunksize:600}` 等会与宽松绑定撞名，导致启动失败。本地配置建议直写默认值，Apollo 使用不撞名的 key（如 `rag.index.chunk.size`）。

---

## 🗓️ 实施阶段

| 阶段 | 内容 |
|------|------|
| Phase 1 | 告警 Tool + MCP + 关键词路由 |
| Phase 1b | DeepSeek Tool Calling |
| Phase 1c | 运维 md 注入 system prompt |
| Phase 1d | RAG（Qwen Embedding + Qdrant + search_knowledge） |

---

## 🗓️ 下一步计划

- [ ] 接入公司 Wiki / KB，支持 PDF、Word、Confluence ETL
- [ ] 关闭 Phase 1c 整份注入，演示纯 RAG
- [ ] 建立 RAG 评估集，量化召回率
- [ ] 多轮会话记忆、告警写操作（需人工确认）

---

## 📁 知识库文档

默认加载 `src/main/resources/knowledge/` 下的 Markdown：

- `01-告警状态说明.md`
- `02-告警级别与响应.md`
- `03-常见告警处理SOP.md`

---

## 📬 联系 / 参考

- GitHub：<https://github.com/fengyl07/agent-server>
- 更多学习笔记见 `docs/alert-agent-learning-notes.md`

---

## 📄 License

内部项目 / 优云 Alert 产品线 POC，按公司规范使用。
