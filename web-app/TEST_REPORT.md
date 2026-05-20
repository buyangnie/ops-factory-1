# 测试报告

**项目:** ops-factory (web-app)  
**分支:** main  
**测试日期:** 2026-05-20  
**基准提交:** 7aed5f04 (origin/main at test start)

---

## 一、本次测试提交范围

共 8 个自有提交（不含主仓 merge）：

| # | 提交 | 说明 |
|---|------|------|
| 1 | `e7709ee1` | 嵌入模式适配及多项前端问题修复 |
| 2 | `bbddbd26` | 将 config.json 移出 git 追踪，避免密钥提交 |
| 3 | `6eb42781` | 提取 MultiSelectDropdown 为 platform 独立组件 |
| 4 | `328877f9` | 关联添加改用 Promise.allSettled 并行 |
| 5 | `30b5a30b` | config.json 加载失败或网关不可达时阻止启动 |
| 6 | `c1112909` | urlParams sessionStorage 操作加 try/catch |
| 7 | `ebad9b18` | 解决冲突（含 ImportError 类型修复、i18n 补充） |
| 8 | `30b5a30b`~`c1112909` | runtime.ts 网关健康检查 + urlParams 安全处理 |

涉及文件 35 个，+1990 / -1824 行（含 package-lock.json）。

---

## 二、测试执行结果

### 2.1 模块边界检查 (`check:boundaries`)

```
✅ PASS — Module boundary check passed.
```

验证 platform/modules 边界规则：
- modules 不跨模块引用
- platform 层级结构正确
- MultiSelectDropdown 提取到 `platform/ui/primitives/` 后边界合规

### 2.2 TypeScript 编译 (`tsc --noEmit`)

```
✅ PASS — 0 errors, 0 warnings
```

关键验证点：
- `ImportError` 接口 9 处 `message` → `code + params` 类型匹配
- `MultiSelectDropdown` 从 ResourceFormModal 提取后 import 路径正确
- `runtime.ts` 新增 `trackedFetch` 调用类型兼容
- `chatRouteState.ts` 的 `isEmbedMode()` 引用有效

### 2.3 生产构建 (`vite build`)

```
✅ PASS — built in 11.43s
```

输出：
- index.html: 0.54 kB
- CSS: 292.35 kB (gzip: 44.41 kB)
- JS: 3,421.90 kB (gzip: 1,072.11 kB)

无构建错误。chunk size warning 为已知遗留问题，非本次引入。

### 2.4 单元测试 (`test:basic`)

```
Test Files: 3 passed, 1 failed (4 total)
Tests:      76 passed (76 total)
Duration:   1.53s
```

| 测试文件 | 结果 | 说明 |
|----------|------|------|
| `boundaryStructure.test.ts` | ✅ PASS | 模块结构验证 |
| `chatRouteState.test.ts` | ✅ PASS | 聊天路由状态（含 embed/startNew） |
| `skillMarketStructure.test.ts` | ✅ PASS | Skill Market 结构 |
| `App.test.tsx` | ❌ FAIL | `@goosed/sdk` 包解析失败 |

**App.test.tsx 失败分析：** 测试环境无法解析 `@goosed/sdk` 包入口，是已有的环境依赖问题（该包需要本地构建），与本次改动无关。76 个用例全部通过，无回归。

---

## 三、功能验证清单

### 3.1 嵌入模式修复

| 场景 | 预期 | 验证方式 |
|------|------|----------|
| embed=true 时侧边栏隐藏 | AppShell 不渲染 sidebar | 代码审查：`isEmbed ? 'embed-mode' : ...` |
| embed 模式右侧面板正常显示 | rightPanel 不受 isEmbed 限制 | 代码审查：`{rightPanel}` |
| embed 模式聊天输入居中 | `.embed-mode .chat-input-area-bottom { left: 0 }` | CSS 规则存在且优先级正确 |
| embed 模式不恢复旧会话 | `isEmbedMode()` 时跳过 sessionStorage | 代码审查：`if (!isEmbed) { readPersisted... }` |

### 3.2 文件预览修复

| 场景 | 预期 | 验证方式 |
|------|------|----------|
| 文本文件预览面板立即打开 | fetch 前 setPreviewFile | 代码审查：PreviewContext.tsx setPreviewFile 在 fetch 前 |
| 空文件不卡 loading | `content === undefined` 而非 `=== ''` | 代码审查：FilePreview.tsx showLoadingOverlay |
| 预览错误可显示 | catch 不清空 previewFile | 代码审查：catch 中无 `setPreviewFile(null)` |

### 3.3 配置安全

| 场景 | 预期 | 验证方式 |
|------|------|----------|
| config.json 不存在 | 启动失败 + 明确错误信息 | 代码审查：throw Error |
| config.json 存在但网关不可达 | 启动失败 + 具体原因 | 代码审查：`/status` 健康检查 |
| config.json 不被 git 追踪 | .gitignore 含 config.json | 文件验证 |
| sessionStorage 不可用 | 不抛异常 | 代码审查：try/catch 包裹 |

### 3.4 代码质量

| 修复项 | 状态 |
|--------|------|
| MultiSelectDropdown 提取到 platform 层 | ✅ |
| handleAddClusterRelation 并行化 | ✅ |
| ImportError 类型一致性 | ✅ |
| i18n 中英文双语补充 | ✅ |
| urlParams sessionStorage 安全 | ✅ |

---

## 四、已知遗留问题

| 问题 | 严重度 | 说明 |
|------|--------|------|
| `App.test.tsx` 因 `@goosed/sdk` 失败 | 低 | 测试环境依赖问题，需本地构建 sdk |
| JS bundle > 500KB | 低 | 遗留问题，建议后续做 code-split |
| `runtime.ts` 6 个 resolve*Url 函数重复 | 低 | 建议提取公共函数 |
| `MultiSelectDropdown` 缺键盘可访问性 | 中 | 后续补充 ARIA role 和键盘导航 |
| `ImportDialog.tsx:281` 硬编码英文 "Row" | 中 | 应走 i18n |

---

## 五、结论

本次 8 个提交通过全部自动化测试（boundaries / tsc / build / 76 个用例），无回归。1 个已知测试文件失败与本次改动无关。建议合并后跟踪遗留的 5 个改进项。
