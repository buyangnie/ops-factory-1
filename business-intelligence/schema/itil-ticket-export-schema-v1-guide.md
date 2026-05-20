# ITIL 工单导出 Schema v1 使用指南

## 1. 概述

`itil-ticket-export-schema-v1.xlsx` 定义了 ITIL 四大流程工单的标准导出数据结构，目标是让不同 ITSM 平台（ServiceNow、BMC Remedy、HPSM、SMAX、iMOC eTicket 等）通过 adapter 输出统一格式的 Excel 文件，支撑 BI 分析、运维报告生成和跨平台数据对比。

覆盖流程：

- Incident（事件管理）
- Service Request（服务请求）
- Problem（问题管理）
- Change（变更管理）

以及两个 SLA 配置表：

- Incident SLA Criteria（事件 SLA 标准）
- Request SLA Criteria（服务请求 SLA 标准）

## 2. 文件结构

工作簿包含 6 个工作表：

| 工作表 | 用途 | 字段数 |
|--------|------|--------|
| `Incident_Schema` | 事件管理字段定义 | 25 |
| `Service_Request_Schema` | 服务请求字段定义 | 33 |
| `Problem_Schema` | 问题管理字段定义 | 27 |
| `Change_Schema` | 变更管理字段定义 | 32 |
| `Incident_SLA_Criteria_Schema` | 事件 SLA 阈值与目标 | 6 |
| `Request_SLA_Criteria_Schema` | 服务请求 SLA 阈值与目标 | 6 |

每个 Schema 工作表包含 8 列定义信息：

| 列名 | 说明 |
|------|------|
| `field_id` | 字段标识符（snake_case），作为 Excel 列头和数据引用的唯一 key |
| `label_zh` | 中文名称 |
| `label_en` | 英文名称 |
| `data_type` | 数据类型：String / Long text / Number / Integer / Float / DateTime / Boolean / Enum |
| `requirement_level` | Required 或 Optional |
| `allowed_values_or_format` | 枚举值（分号分隔）或格式约束 |
| `example` | 示例值 |
| `definition_or_note` | 字段定义和说明 |

## 3. 数据文件约定

### 3.1 文件命名与组织

每个流程对应一个独立的 Excel 文件，文件放在同一数据目录下：

```
data/
├── Incidents-exported.xlsx      # 事件数据
├── Changes-exported.xlsx        # 变更数据
├── Requests-exported.xlsx       # 服务请求数据
├── Problems-exported.xlsx       # 问题数据
└── SLAs-exported.xlsx           # SLA 配置（含 Incidents_SLA + Requests_SLA 两个 sheet）
```

### 3.2 Sheet 命名

- 工单数据文件的 Sheet 名统一为 `Data`
- SLA 配置文件的 Sheet 名为 `Incidents_SLA` 和 `Requests_SLA`

### 3.3 列头规则

- 列头使用 `field_id`（snake_case），如 `ticket_id`、`opened_at`、`assigned_to`
- Optional 字段即使为空也必须保留列（值为空字符串）
- 列顺序应与 Schema 工作表中的定义顺序一致

### 3.4 数据格式

| 数据类型 | 格式要求 |
|----------|----------|
| String | 纯文本 |
| Long text | 支持多行长文本 |
| Number | 数值类型，支持小数 |
| Integer | 整数 |
| Float | 浮点数（如 SLA 目标 0.95 表示 95%）|
| DateTime | `YYYY-MM-DD HH:MM:SS`，如 `2026-03-01 09:00:00` |
| Boolean | `TRUE` / `FALSE` |
| Enum | 使用 `allowed_values_or_format` 中定义的枚举值 |

## 4. 四大流程字段详解

### 4.1 通用字段（4 个流程共享）

以下字段在所有流程中含义一致：

| field_id | 说明 | Required |
|----------|------|----------|
| `ticket_id` | 工单编号，如 INC0010001、CHG0010001、REQ0010001、PRB0010001 | Yes |
| `title` | 工单标题 | Yes |
| `description` | 详细描述 | No |
| `status` | 生命周期状态 | Yes |
| `status_reason` | 状态原因 | No |
| `priority` | 优先级 P1-P4 | Yes |
| `category` | 一级分类 | Yes |
| `subcategory` | 二级分类 | No |
| `channel` | 创建渠道 | No |
| `requester` | 请求人/报告人 | Yes |
| `affected_user` | 受影响人 | No |
| `affected_item` | 受影响配置项 (CI) | No |
| `assigned_group` | 处理组 | No |
| `assigned_to` | 当前处理人 | Yes |
| `opened_at` | 打开时间 | Yes |
| `updated_at` | 更新时间 | No |
| `resolved_at` | 解决时间 | No |
| `closed_at` | 关闭时间 | No |

### 4.2 Incident（事件管理）— 25 个字段

**流程特有字段：**

| field_id | 说明 | 重点说明 |
|----------|------|----------|
| `response_time_minutes` | 响应耗时（分钟） | >= 0 |
| `resolution_time_minutes` | 解决耗时（分钟） | >= 0，从 opened_at 到 resolved_at |
| `suspended_minutes` | 挂起时长（分钟） | >= 0 |
| `close_code` | 关闭代码 | `Solved; Workaround; Duplicate; Not_reproducible; Cancelled` |
| `close_notes` | 关闭说明 | |
| `major_incident` | 是否重大事件 | Boolean: `TRUE` / `FALSE` |
| `problem_ids` | 关联问题单 | 如 `PRB0001` |

**状态枚举：** `New; Open; Pending; Resolved; Closed; Cancelled`

### 4.3 Service Request（服务请求）— 33 个字段

**流程特有字段：**

| field_id | 说明 | 重点说明 |
|----------|------|----------|
| `response_time_minutes` | 响应耗时（分钟） | >= 0 |
| `resolution_time_minutes` | 履约耗时（分钟） | >= 0，**单位是分钟不是小时** |
| `suspended_minutes` | 挂起时长（分钟） | >= 0 |
| `close_code` | 关闭代码 | `Fulfilled; Cancelled` |
| `close_notes` | 关闭说明 | |
| `catalog_item` | 服务目录项 | **Required**，如 VPN Access、Database Read Access |
| `approval_status` | 审批状态 | `Not Required; Pending; Approved; Rejected` |
| `approver` | 审批人 | |
| `fulfilled_at` | 履约完成时间 | |
| `request_variables` | 请求变量 | JSON 格式，如 `{"os":"macOS"}` |
| `satisfaction_score` | 满意度评分 | **Required**，整数 >= 0，通常 1-5 分 |
| `requester_dept` | 请求人部门 | **Required** |
| `feedback` | 用户反馈 | |
| `change_ids` | 关联变更单 | |
| `incident_ids` | 关联事件单 | |

**状态枚举：** `New; Open; Pending; Resolved; Closed; Cancelled`

**注意事项：**
- `resolution_time_minutes` 是分钟单位。如需换算为小时需除以 60。
- `satisfaction_score` 是必填字段，如果用户未评分 adapter 应输出默认值（如 0 或空）。
- Request 的 SLA 达标率不依赖 `SLA Met` 字段，而是通过比较 `resolution_time_minutes` 与 `Requests_SLA` 中对应优先级的 `resolution_sla_min` 阈值来计算。

### 4.4 Problem（问题管理）— 27 个字段

**流程特有字段：**

| field_id | 说明 | 重点说明 |
|----------|------|----------|
| `close_code` | 关闭代码 | `Fix_applied; Risk_accepted; Duplicate; Cancelled` |
| `close_notes` | 关闭说明 | |
| `known_error` | 是否已知错误 | **Boolean**：`TRUE` / `FALSE`（不是 Yes/No 字符串）|
| `root_cause` | 根因描述 | 长文本 |
| `cause_code` | 原因代码 | 如 `Technical Defect; Human Error; Process Gap; Vendor Issue` |
| `workaround` | 临时规避方案 | **非空文本表示有规避方案**（不是 Yes/No） |
| `permanent_fix` | 永久修复描述 | **非空文本表示已实施永久修复**（不是 Yes/No） |
| `related_incident_count` | 关联事件数量 | 整数 >= 0 |
| `permanent_fix_change_id` | 永久修复变更单号 | 如 `CHG0010001` |

**状态枚举：** `New; Open; Pending; Resolved; Closed; Cancelled`

**注意事项：**
- `known_error` 是布尔类型，不是字符串 "Yes"/"No"。Adapter 输出时需转换为 `TRUE`/`FALSE`。
- `workaround` 和 `permanent_fix` 的语义从"是否有"变为"方案内容"：字段值为非空文本时表示有对应方案，为空表示没有。
- Problem 没有单独的 `response_time_minutes` / `resolution_time_minutes` 字段，可通过 `opened_at` 和 `resolved_at` 计算耗时。

### 4.5 Change（变更管理）— 32 个字段

**流程特有字段：**

| field_id | 说明 | 重点说明 |
|----------|------|----------|
| `close_code` | 关闭代码 | `Successful; Failed; Backed_out; Cancelled` |
| `close_notes` | 关闭说明 | |
| `approval_status` | 审批状态 | `Not Required; Pending; Approved; Rejected` |
| `approver` | 审批人 | |
| `change_type` | 变更类型 | **Required**，`Standard; Normal; Emergency` |
| `risk` | 风险等级 | **Required**，`Low; Medium; High` |
| `planned_start_at` | 计划开始时间 | **Required** |
| `planned_end_at` | 计划结束时间 | **Required** |
| `actual_start_at` | 实际开始时间 | |
| `actual_end_at` | 实际结束时间 | |
| `implementation_plan` | 实施计划 | 长文本 |
| `test_plan` | 测试计划 | 长文本 |
| `backout_plan` | 回退计划 | 长文本 |
| `incident_ids` | 关联事件单 | **非空表示变更引发了事件**（不是 Yes/No 字段） |

**状态枚举：** `New; Open; Pending; Resolved; Closed; Cancelled`

**注意事项：**
- `close_code` 中的 `Successful` 表示变更成功（不是 "Yes"），`Failed` 表示失败（不是 "No"）。
- `incident_ids` 的语义：字段值非空（包含逗号分隔的事件编号）表示该变更引发了关联事件。空值表示未引发事件。不再使用 `Incident Caused: Yes/No` 形式。
- 变更持续时间通过 `actual_end_at - actual_start_at` 计算，单位为小时。

## 5. SLA 配置表

### 5.1 概述

SLA 配置独立存放在 `SLAs-exported.xlsx` 中，包含 `Incidents_SLA` 和 `Requests_SLA` 两个 sheet，分别定义事件和服务请求的 SLA 标准。

### 5.2 字段定义（2 个 SLA 表结构相同）

| field_id | 说明 | 类型 | 示例 |
|----------|------|------|------|
| `priority` | 工单优先级 | String | `P1` |
| `response_sla_min` | 响应 SLA 阈值（分钟） | Integer | `60` |
| `response_sla_target` | 响应 SLA 达标率目标 | Float | `0.95`（表示 95%） |
| `resolution_sla_min` | 解决 SLA 阈值（分钟） | Integer | `240` |
| `resolution_sla_target` | 解决 SLA 达标率目标 | Float | `0.90`（表示 90%） |
| `time_basis` | 计时方式 | Enum | `Calendar; Business_hours; Business_days` |

### 5.3 SLA 达标判定逻辑

对每条工单，SLA 是否达标的判定规则为：

```
SLA 达标 = (response_time_minutes <= response_sla_min) AND (resolution_time_minutes <= resolution_sla_min)
```

**注意：** SLA 阈值单位统一为分钟。旧版 incident 导出中的 `Resolution（hours）` 需要乘以 60 转换为分钟。

### 5.4 time_basis 计时方式说明

| 值 | 说明 | 典型场景 |
|----|------|----------|
| `Calendar` | 7×24 日历时间连续计时 | P1 事件 15 分钟响应 |
| `Business_hours` | 仅在工作时间内计时（如周一至周五 9:00-18:00） | 一般事件 4 小时响应 |
| `Business_days` | 按工作日计时 | 3 个工作日内解决 |

### 5.5 SLA 目标达标率

`response_sla_target` 和 `resolution_sla_target` 存储为小数（0.0-1.0），表示该优先级的 SLA 达标率目标：

- `0.95` 表示 95% 的工单应在 SLA 阈值内完成响应/解决
- BI 分析时可对比实际达标率与目标值的差距，输出 SLA 达标趋势报告

## 6. 适配器开发指南

### 6.1 Adapter 职责

Adapter 的核心任务是将平台原始数据转换为符合本 Schema 定义的标准化 Excel 文件。

### 6.2 开发步骤

1. **字段映射**：根据各流程 Schema 工作表，将平台原始字段映射到标准 `field_id`
2. **枚举归一化**：将平台特有的状态、优先级等枚举值转换为标准枚举
   - 如平台使用 "Completed" → 统一转为 "Closed"
   - 如平台使用 "High/Medium/Low" 优先级 → 转换为 "P1/P2/P3/P4"
3. **数据类型转换**：
   - 时间字段统一为 `YYYY-MM-DD HH:MM:SS` 格式
   - Boolean 字段统一为 `TRUE`/`FALSE`（不是 Yes/No/1/0）
   - 时间单位统一为分钟（小时需 ×60）
4. **Optional 字段处理**：源系统中不存在的 Optional 字段留空，但保留列
5. **SLA 配置**：独立生成 SLA 配置文件，按优先级定义阈值

### 6.3 常见适配注意事项

| 场景 | 处理方式 |
|------|----------|
| 源系统没有 `close_code` 字段 | 根据 status 推断：Closed → 空，Resolved → 空 |
| 源系统 `workaround` 是 Yes/No | 转换为非空文本（有方案时输出方案描述） |
| 源系统 `known_error` 是 Yes/No 字符串 | 转换为 Boolean `TRUE`/`FALSE` |
| 源系统时间单位为小时 | 乘以 60 转为分钟（`response_time_minutes`、`resolution_time_minutes`） |
| 源系统 `Success` 字段是 Yes/No | 转换为 `close_code`：Yes → `Successful`，No → `Failed` |
| 源系统 `Incident Caused` 是 Yes/No | 转换为 `incident_ids`：Yes → 填入关联事件 ID，No → 留空 |
| 源系统日期列名不统一（Begin Date / Requested Date / Logged Date） | 统一映射为 `opened_at` |
| 源系统处理人字段不统一（Resolver / Implementer / Assignee） | 统一映射为 `assigned_to` |

## 7. 数据校验

### 7.1 Required 字段检查

导入数据前应校验所有 Required 字段不为空：

- 四流程通用 Required：`ticket_id`, `title`, `status`, `priority`, `category`, `requester`, `assigned_to`, `opened_at`
- Service Request 额外 Required：`catalog_item`, `satisfaction_score`, `requester_dept`
- Change 额外 Required：`change_type`, `risk`, `planned_start_at`, `planned_end_at`

### 7.2 枚举值校验

所有 Enum 类型字段应仅包含 `allowed_values_or_format` 中定义的值。不在枚举范围内的值应被标记或拒绝。

### 7.3 时间逻辑校验

```
opened_at <= resolved_at <= closed_at
planned_start_at <= planned_end_at
actual_start_at <= actual_end_at
response_time_minutes >= 0
resolution_time_minutes >= 0
suspended_minutes >= 0
```

### 7.4 跨流程关联校验

- `incident_ids` 中引用的工单编号应能在 Incidents 数据中找到
- `problem_ids` 中引用的工单编号应能在 Problems 数据中找到
- `change_ids` 中引用的工单编号应能在 Changes 数据中找到
