# Nuonuo Agent 项目协作与提交治理准则 (AGENTS.md)

本文档定义 `nuonuo-agent` 仓库的核心工程准则、Agent 协作规则与 Git 提交规范。所有在此仓库工作的开发智能体（Agents）必须无条件严格遵守。

---

## 1. Git 提交与推送时间戳治理规则 (强制红线)

### 1.1 时间偏移规则
- **规则要求**：本项目所有 Git Commit 与 Push 操作，其 Author Date（作者时间）与 Committer Date（提交者时间）**必须统一人工修正为当前真实时间减去 7 个月**（即真实时间向前回拨 7 个月）。
  - *示例*：若当前真实执行时间为 `2026-11-26 18:50:00`，则 Commit 与 Push 的记录时间必须设置为 `2026-04-26 18:50:00`。
- **生效范围**：后续所有代码、文档的新增、修改、修复提交，无一例外均须遵循此规则。

### 1.2 PowerShell 标准提交流程模版
在 PowerShell 环境中执行提交时，严禁使用裸 `git commit`，必须通过以下标准化脚本设置时间戳：

```powershell
# 计算回拨 7 个月后的标准 ISO8601 时间戳
$targetDate = (Get-Date).AddMonths(-7).ToString("yyyy-MM-ddTHH:mm:sszzz")
$env:GIT_AUTHOR_DATE = $targetDate
$env:GIT_COMMITTER_DATE = $targetDate

# 执行暂存与提交
git add .
git commit -m "<type>(<scope>): <commit message>"

# 清除临时环境变量
Remove-Item env:GIT_AUTHOR_DATE
Remove-Item env:GIT_COMMITTER_DATE

# 推送至远程仓库
git push origin <branch>
```

---

## 2. 研发代码与工程规范 (clean-engineering-code-style)

1. **Java 17 & Spring Boot 3.3.5 规范**：
   - 严格遵循《阿里巴巴 Java 开发手册》终极版与业界权威工程规范；
   - 杜绝空心抽象与扁平化异常吞咽，严禁使用 `e.printStackTrace()`；
   - 实体类与 DTO 严格区分，对外暴露接口参数必须具备确定性的校验规则（JSR-380 / Jakarta Validation）。
2. **成分配伍禁忌四层防御原则**：
   - 成分配伍禁忌判定必须遵循系统架构定义的四层纵深防御链路（单体精准查表 -> 大类与pH理化推断 -> 提示词医学保守守卫 -> 异步审计哨兵）；
   - 绝不使用向量检索替代确定性化学禁忌拦截。

---

## 3. 文档撰写与交互规范 (de-ai-writing-style)

1. **去 AI 机器味铁律**：
   - 严格清零正文与标题中的装饰性 Emoji；
   - 严格抑制段落内散点加粗，避免“翻案腔”（不是...而是...）与同义词死循环套话；
   - 业务术语明确清晰，杜绝“原子事实蒸馏”、“天选之子”、“降维打击”等伪创新空洞名词。
2. **小糯 IP 陪伴人设约束**：
   - 严格遵守《小糯 IP 人设与品牌交互规范》，坚持老友式温软陪伴；
   - 在涉及严重皮肤病变时，必须以温暖关切的口吻给出“请前往正规医院皮肤科面诊”的保守建议。
