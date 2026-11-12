# 懒记 Lazee V1 开发规格书

Nov 12, 2026 · @SW'S BABY

这份文档是交给 Claude Code 的完整开发说明：照它一次做完 V1 全部功能，用 GitHub Actions 编译出可安装的 APK。界面以原型为准（仓库里的 `docs/prototype.html`）。

## 0. 给 Claude Code 的开发指引

**任务**：在这个仓库里从零做出完整的 V1 Android App，本文档列出的功能一次全部做完，最后由 GitHub Actions 编译出可以直接安装的 APK。

1. **设计以原型为准**：界面、文案、交互、颜色请对照 `docs/prototype.html`。它的标记和脚本里写明了每个画面的布局、尺寸、颜色和中英文文案。原型和本文档冲突时，以本文档为准。原型里没有的画面（第 5 节标「原型外」的），沿用同一套卡片、列表、开关、分段控件风格自行设计。
2. **先读完整份文档再动手**，然后按第 13 节的里程碑顺序开发。
3. **能用库就用库**：用第 2 节列的成熟库，不要自己重写已有的功能。要加新的库，先确认它不需要网络，并在 `docs/DECISIONS.md` 记一行原因。
4. **好读好改**：代码、注释、commit 信息用英文；每个文件尽量不超过 300 行；命名直白；复杂的地方写一两行注释说明「为什么」。用户看得到的文字一律放 `strings.xml`（英文 + 中文），代码里不准写死。
5. **绝对不要加 `INTERNET` 权限**，也不要加任何分析、广告、崩溃上报 SDK。App 是完全离线的，这是产品的卖点。
6. **怎么验证**：你的环境可能没有 Android SDK。以 GitHub Actions 的结果为准：每完成一个里程碑就 commit 并 push，用 `gh run watch` 等它跑完；失败就用 `gh run view --log-failed` 看原因，修到通过再继续下一个里程碑。本地有 SDK 时，先跑 `./gradlew testDebugUnitTest assembleDebug`。
7. **拿不准的地方**：选最简单、最安全的做法，继续做，并在 `docs/DECISIONS.md` 记一行。不要为小事停下来。
8. **在仓库根目录建 `CLAUDE.md`**：写上本文档的第 0 节和第 3 节的架构规则，让以后的会话也遵守。本文档本身保存为 `docs/SPEC.md`。
9. **做完时**：更新 `README.md`（中文），写清楚怎么下载 APK、怎么安装、怎么开通知权限（含「允许受限制的设置」），并把第 13 节的验收清单逐项打勾。

## 1. 产品概述与原则

一个只在 Android 上运行、完全离线的个人记账 App，给马来西亚用户用（货币 RM）。主要的记账方式是「读取银行 / 电子钱包的付款通知自动记账」，手动记账是补充。

- **App 名称**：中文「懒记」，英文「Lazee」。`values/strings.xml` 的 `app_name` = `Lazee`，`values-zh/strings.xml` 的 `app_name` = `懒记`。定位语：给懒人用的记账 App，你睡觉它也在帮你记。
- **applicationId**：`io.github.<github用户名>.lazee`（从仓库拥有者名字换小写得出）。
- **用户**：懒得手动记账的学生和上班族，常用 TNG eWallet、MAE 等。
- **语言**：英文 + 简体中文，在 App 内切换。

五个产品原则，开发时遇到取舍就看这里：

1. **懒人优先**：能自动就自动，能少点一下就少点。
2. **隐私优先**：不上网、资料加密、只存必要的字段。
3. **不重复记账**：同一笔钱只能入账一次（通知、账单、手动三个来源都要互相去重）。
4. **什么都可以改、可以撤销**：自动记的账都标「自动」，都能改、能删。
5. **简单**：宁可少一个设置，也不要让界面变复杂。

## 2. 技术栈与依赖库

用 Android 官方推荐的标准做法：Kotlin + Jetpack Compose。所有版本号用开发当天的最新稳定版，统一写在 `gradle/libs.versions.toml`。

| 用途 | 用什么 | 备注 |
| --- | --- | --- |
| 语言 | Kotlin | Gradle 用 Kotlin DSL + version catalog |
| 界面 | Jetpack Compose + Material 3 | 5 个主题 = 5 组 ColorScheme + 自定义颜色（第 9 节） |
| 页面切换 | Navigation Compose | 底部 4 个分页 + 各子页面 |
| 依赖注入 | Hilt | 官方方案，文档最多 |
| 资料库 | Room | DAO + Flow；表结构见第 4 节 |
| 资料库加密 | SQLCipher for Android（`net.zetetic:sqlcipher-android`） | 通过 Room 的 `openHelperFactory` 接入 |
| 密钥保管 | Android Keystore（系统自带） | 随机生成数据库密码，用 Keystore 里的 AES 密钥加密后存在 DataStore |
| 设置 | DataStore Preferences | 语言、主题、开关、备份设置 |
| 图表 | Vico（Compose 版） | 折线图、柱状图；日历热力图和分类条用普通 Compose 画 |
| 指纹 / PIN 锁 | androidx.biometric | `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` |
| 后台任务 | WorkManager | 账单提醒、每晚汇总、自动备份、月度总结 |
| 读取通知 | NotificationListenerService（系统自带） | 见第 6 节 |
| 发通知 | NotificationCompat（androidx.core） | 带「确认 / 改分类 / 忽略」按钮 |
| 备份到文件 / Google Drive | Storage Access Framework（系统自带） | `OpenDocumentTree` 选文件夹，不需要网络权限 |
| JSON | kotlinx.serialization | 备份格式、商家词库、解析规则 |
| 导出 CSV | kotlin-csv（`com.github.doyaaaaaken:kotlin-csv-jvm`） | 带 UTF-8 BOM，Excel 打开中文不乱码 |
| 计算器 | exp4j | 把 `× ÷ −` 换成 `* / -` 再算 |
| 选照片 | Photo Picker（`PickVisualMedia`）+ 相机 `TakePicture` | 不需要存储权限 |
| 日期 | java.time（启用 core library desugaring） |  |
| 测试 | JUnit 4 + kotlinx-coroutines-test + Turbine + Truth | Room 测试用 Robolectric |

- **minSdk 26**（Android 8.0）；**compileSdk / targetSdk** 用 Google Play 当前要求的版本。
- **JDK 17**。
- **字体要打包进 App**：Inter 和 Noto Sans SC 放在 `res/font/`（两者都是 SIL OFL 许可，可以打包）。不要用可下载字体，因为 App 没有网络。Noto Sans SC 很大，只打包 Regular / Medium / Bold 三个字重。拿不到字体文件时，先用系统默认字体，并在 `docs/DECISIONS.md` 记下来。

## 3. 项目结构与架构规则

单模块 `app`，按功能分文件夹。要改哪个功能，就只看对应的文件夹。

```
app/src/main/java/.../lazee/
  LazeeApp.kt              Application，Hilt 入口
  MainActivity.kt           单 Activity，挂 NavHost，处理 App 锁
  core/
    data/                   Room: AppDatabase, entities, DAOs, migrations
    repo/                   Repository：界面只通过它拿资料
    prefs/                  DataStore 设置
    security/               Keystore、数据库密码、备份加密
    money/                  Money（以「分」为单位的 Long）、格式化
    calc/                   计算器（包装 exp4j）
    ui/theme/               颜色、主题、字体、形状
    ui/components/          共用组件：分类圆球、开关行、分段控件、卡片…
  feature/
    home/  add/  stats/  budget/  me/  accounts/  search/
    bills/  loans/  goals/  categories/  backup/  settings/  onboarding/
    autolog/
      listener/             NotificationListenerService
      parser/               ParserEngine + 每个 App 的规则
      classify/             分类器（商家记忆 > 词库 > 关键词）
      match/                去重、转账配对、账单配对
      pending/              待确认画面 + 通知按钮的 BroadcastReceiver
  work/                     所有 WorkManager Worker
app/src/main/assets/
  merchants.json            商家 → 分类 词库
  keywords.json             关键词 → 分类 规则
  parsers/*.json            每个 App 的通知解析规则
app/src/test/resources/notifications/   通知样本（已遮蔽）
docs/  SPEC.md  DECISIONS.md  prototype.html
.github/workflows/android.yml
```

**架构规则**

1. **每个画面分两层**：`XxxScreen.kt`（只管界面，接收 state，发出事件）和 `XxxViewModel.kt`（管逻辑，用 `StateFlow<XxxUiState>` 暴露状态）。
2. **资料流向一致**：Room DAO 返回 `Flow` → Repository → ViewModel 组合成 UiState → Screen。界面不直接碰 DAO。
3. **金额一律用 `Long`，单位是分**（RM 6.50 = 650），绝不用 Float / Double 存钱。只有显示时才格式化为 `RM 6.50`。
4. **纯逻辑和 Android 分开**：解析、分类、去重、账单配对、预算计算、计算器都写成不依赖 Android 的普通 Kotlin 类，方便单元测试。
5. **配置放 JSON，不放代码**：商家词库、关键词、解析规则都在 `assets/`，以后加商家或银行改格式时只改 JSON。
6. **文字放 strings.xml**，颜色、尺寸放 `core/ui/theme`。
7. **资料库版本从 1 开始**，以后改表必须写 Migration，不准用 `fallbackToDestructiveMigration`（会清空用户资料）。

## 4. 数据模型

Room 表如下。金额全部是 `Long`（分），日期用 `LocalDate`（存 epochDay），时间用 `Instant`（存毫秒）。每张表都有 `id: Long` 自增主键，下面不再重复。

| 表 | 字段 | 说明 |
| --- | --- | --- |
| Account | name, type(CASH / BANK / EWALLET / CREDIT\_CARD), openingBalance, statementDay?, dueDay?, sortOrder, archived | 信用卡才有账单日、还款日；余额 = openingBalance + 相关交易实时算 |
| Category | builtInKey?, customName?, kind(EXPENSE / INCOME), colorIndex(0–7), parentId?, sortOrder, archived | 内置分类用 builtInKey 取翻译；用户新建的用 customName；parentId 有值就是二级分类 |
| Transaction | type(EXPENSE / INCOME / TRANSFER), amount, categoryId?, accountId, toAccountId?, date, createdAt, note?, merchant?, source(MANUAL / NOTIFICATION / BILL / LOAN), isAuto, billId?, loanId?, photoPath? | 转账不算收支；isAuto = 未经确认自动入账 |
| Tag / TransactionTag | name / (transactionId, tagId) | 备注里 `#旅行` 这样的词自动变成标签 |
| Template | name, type, amount?, categoryId, accountId, note? | 常用记账模板 |
| CategoryBudget | categoryId, limit | 每月生效；总预算存在 DataStore |
| SavingGoal | name, target, deadline?, colorIndex, archived |  |
| GoalContribution | goalId, amount, date | 存入为正、取出为负 |
| Bill | name, amount, dayOfMonth(1–31), remindDaysBefore(默认 2), mode(REMIND / AUTO\_RECORD), categoryId, accountId?, isSubscription, totalInstallments?, startMonth, active | 一张表包办「账单提醒 / 周期性记账 / 订阅 / 分期」，见第 5 节 |
| BillPayment | billId, yearMonth, transactionId?, method(NOTIFICATION / MANUAL / AUTO) | 一笔账单每月最多一条 |
| Loan | person, direction(LENT / BORROWED), principal, annualRatePercent?, startDate, dueDate?, accountId, settled | 单利计算 |
| LoanRepayment | loanId, amount, date, transactionId |  |
| PendingTxn | sourcePackage, merchant, amount, direction(OUT / IN), postedAt, suggestedCategoryId?, confidence(HIGH / MEDIUM / LOW), matchedBillId?, possibleDuplicateOf?, transferPairId?, status(PENDING / CONFIRMED / IGNORED), dedupeKey(unique) | 不存通知原文 |
| MerchantRule | merchantKey(unique), categoryId, confirmCount, alwaysAsk | 用户的「商家记忆」 |

**DataStore 设置**：language(zh / en)、theme(green / blue / terra / grape / ink)、hideAmounts、appLockEnabled、monthlyBudget、autoLogEnabled、enabledPackages(Set)、lazyQuickConfirm、lazyAutoFamiliar、nightlySummary、nightlySummaryTime(默认 21:00)、budgetAlerts、autoBackup、backupFrequency(DAILY / WEEKLY)、backupTreeUri、lastBackupAt、onboardingDone、dbPassphraseEncrypted。

**第一次启动时建好的内置资料**：

- 账户：现金（其它账户在引导页让用户勾选：TNG eWallet、MAE、信用卡…）。
- 支出分类：餐饮(0)、交通(1)、购物(2)、娱乐(3)、账单(4)、教育(5)、人情(6)、其它(7)。括号里是 colorIndex。
- 收入分类：津贴(4)、兼职(4)、礼金(6)、其它(7)。
- 英文名：Food, Transport, Shopping, Fun, Bills, Study, Gifts, Other；Allowance, Part-time, Gift money, Other；转账 = Transfer。

## 5. 功能规格：每个画面

底部导航 5 格：明细 · 统计 · \[+ 记一笔\] · 预算 · 我的。中间的 + 是圆形主题色按钮。子页面不显示底部导航，左上角有返回钮。标「原型外」的画面在原型里没有，照同一风格设计。

### 5.1 引导页（原型外，只在第一次打开时）

1. 选语言（中文 / English）。
2. 勾选你有的账户（现金默认勾上），可填当前余额。
3. 设每月总预算（可跳过）。
4. 介绍通知自动记账：说明会读什么、不读什么、资料只存本机；「开启」走第 5.10 节的授权流程，「以后再说」直接进入首页。
5. Android 13 以上请求通知权限 `POST_NOTIFICATIONS`。

### 5.2 明细（首页）

- **顶部**：大字月份（中文「11 月」、英文「Nov」），旁边一个主题色小圆点和年份。下面 7 个小点代表本周一至周日：有记账的日子实心，今天外面加一圈，不配任何文字（纯装饰 + 暗示）。右上角：搜索按钮（原型外，放在眼睛左边）、眼睛按钮（全 App 隐藏金额，显示成 `RM ••••`，状态存 DataStore）。
- **深色大卡**：本月支出、预算进度条、预算剩余、本月收入。没设预算时进度条换成「设置预算」链接。
- **黄色横条**：有待确认账单时显示「N 笔来自通知的账单待确认」，点进待确认页。
- **白色横条**：本月最近一笔未付账单「N 天后到期 · 名称 · 金额」，点进账单提醒。
- **明细列表**：本月所有记录，按日分组（今天 / 昨天 / 日期），组头显示当日支出。每行：分类圆球（分类名首字）、备注（没备注就显示商家，再没有就显示分类名）、「自动」小标签、「分类 · 账户」、金额（支出黑色带 `-`，收入主题色带 `+`，转账灰色不带符号）。列表底部「查看上个月」可一直往前翻。
- **点一行**→ 交易详情（原型外）：所有字段、照片、标签、来源；操作：编辑、删除、再记一笔（复制到记账页）、存成模板。自动记的账还有「撤销自动记账」。
- **长按一行**进入多选：批量删除、批量改分类。

### 5.3 记一笔

- 顶部：关闭、支出 / 收入 / 转账 分段控件。
- 模板行（原型外）：有模板时，在金额上方显示最多 5 个模板小圆角按钮，点一下填好分类、账户、备注、金额。
- **日期不用输入**：默认今天。点日期按钮依次切换今天 → 昨天 → 前天；长按打开日期选择器。
- **金额显示**：右上大字 `RM 0.00`；有算式时，上方小字显示算式（如 `12 + 8.5`），大字显示结果。
- **分类格**：5 列，圆球 + 名称，选中的外面加主题色圈。最后一格是虚线圈「新增」→ 新增分类页，存好后自动选中并回到记账页。有二级分类的分类被选中时，下方展开一行子分类小按钮（可不选）。转账模式不显示分类，改成「从 → 到」两个账户选择。
- **账户行**：小圆角按钮横向滚动，默认上次用的账户。
- **备注输入框**：可不填；写 `#词` 自动变成标签。右边一个相机图标可加照片（拍照或从相册选），照片压缩到长边 1600px 存 App 私有目录。
- **计算器键盘**（4 列）：`7 8 9 ÷ / 4 5 6 × / 1 2 3 − / . 0 ⌫ +`，运算符键用主题浅色底。规则：开头不能是运算符；连按两个运算符时后一个取代前一个；每个数最多 2 位小数、7 位整数；结果四舍五入到分；除以 0 结果为 0 并提示。
- **保存按钮**：有未算完的算式时显示「= 计算」，按一下算出结果；再按「保存」入账。金额 ≤ 0 时按钮灰色不可点。保存后回首页并弹提示「已记一笔 RM x」。
- 保存时检查预算：达到分类或总预算的 80% / 100% 时发一次提醒（同一个月同一级别只提醒一次）。

### 5.4 统计

顶部标题 +「‹ 2026 年 11 月 ›」月份切换。从上到下：

1. **收支总览**：支出、收入、结余 + 储蓄率（(收入−支出)/收入，小于 0 显示 0%）。
2. **花钱速度**：折线图，x = 当月第几天，y = 累计支出。实线本月、虚线上月、红色虚横线预算。上方一句话：「比上月同期多 / 少花 RM x。照这个速度，月底约 RM y，会超出 / 在预算内」。预测公式：本月账单类支出 + (其余支出 ÷ 已过天数 × 当月天数)。会超出时这句话用警告色。
3. **分类**：顶部一条按占比分段的横条（用主题的图表色阶），下面每个分类一行：圆球、名称、百分比、金额、进度条，按金额从大到小。点一行 → 分类明细。
4. **商家排行 Top 5**：按金额排序，显示次数和进度条（只算有 merchant 的记录）。
5. **每日花费日历**：7 列（星期一开始），每天一格，按花费分 5 档底色（无 / <30 / <60 / <120 / ≥120，最深一档白字），今天加主题色圈，未来的日子虚线框。右上角「无消费日 N 天」。
6. **花钱习惯 4 小格**：支付方式占比（按账户）、平日 vs 周末日均、固定（账单分类 + Bill 来源）vs 日常、RM 10 以下小钱的笔数和总额（「≈ N 天的预算」）。
7. **近 6 个月收支**：每月两根柱子（收入浅色、支出深色，当月支出半透明）。
8. **年度回顾**卡片：12 月 31 日之前显示「12 月 31 日开放」，之后点进年度页（原型外）：全年收支、月度柱状图、前 5 分类、前 5 商家、存下的总额。

**分类明细**：圆球 + 名称、本月金额、占比和预算、近 6 个月柱状图、本月每一笔。

### 5.5 搜索与筛选（原型外）

关键词（备注、商家、标签）+ 筛选：类型、分类、账户、日期范围、金额范围、只看自动记录。结果列表和首页一样，顶部显示笔数和合计。

### 5.6 预算

- 大卡：本月还可以花、总预算、进度条、「剩 N 天，平均每天可花 RM x」（剩余预算 ÷ 含今天的剩余天数）。点「总预算」可修改。
- 分类预算列表：名称、剩余或「超支 RM x」、进度条（<80% 主题色、≥80% 橙色 #E39A1E、超支红色 #D92D20）、「已用 x / y」。底部「+ 设置分类预算」。
- **存钱目标**（原型外，放在预算页下半部）：每个目标一张卡片：名称、已存 / 目标、进度条、期限和「每月还要存 RM x」。按钮：存入 / 取出。存入不算支出，只是记录。

### 5.7 我的

- **净资产卡**（原型外，放最上面）：资产（非信用卡账户余额 + 借出未还）− 负债（信用卡欠款 + 借入未还）。
- **账户列表**：名称、余额；点进账户详情（原型外）：该账户的记录、编辑、归档；信用卡显示账单日、还款日、「本期要还 RM x」（上一个账单日之前的欠款），并可一键生成对应的「信用卡还款」账单提醒。「+ 新增账户」。
- **设置列表**（每行右边一个状态小标签）：通知自动记账、账单提醒（N 笔待付）、借还款（原型外）、分类管理、备份与导出、外观与语言、提醒设置（原型外：每晚汇总时间、预算提醒开关）、App 锁（开关）、关于（版本号、隐私说明、开源库许可）。

### 5.8 账单提醒（含周期性记账、订阅、分期）

- 顶部蓝色说明框：「提醒只负责提醒，不会自己记账…不会重复记两次」（见原型）。
- 分段控件（原型外）：全部 / 订阅。「订阅」只显示 isSubscription 的账单，顶部显示「每月订阅合计 RM x」。
- 列表：未付的按到期日排前面，已付的在后面。每张卡：日期方块、名称、「每月 N 日 · 提前 N 天提醒」、金额、状态（N 天后到期 / 今天到期 / 本月已付 · 通知自动配对 / 手动标记）。分期的显示「第 3 / 12 期」。
- 未付的卡下方按钮：「已用现金付了？手动标记已付（会记一笔）」。
- 「+ 新增」→ 编辑页（原型外）：名称、金额、每月几号（31 号遇到短月就算月底）、提前几天提醒、分类、账户、模式（只提醒 / 到期自动记一笔）、是否订阅、分期总期数（可空）。
- 不重复记账的规则见第 6 节第 7 步。

### 5.9 借还款（原型外）

两个分页：我借出 / 我借入。每笔：对方名字、本金、年利率（可空）、开始日、到期日、已还 / 未还。利息用单利：本金 × 年利率 × 天数 / 365，四舍五入到分。记一笔还款会同时产生一条 source = LOAN 的交易（借出、借入本金和还款本金都算转账，不算收支；利息算收入或支出）。还清后自动标记已结清。

### 5.10 通知自动记账设置

照原型：总开关、懒人模式三个开关、通知样式预览、App 白名单（只列出已支持解析的 App）、「你的资料怎么被保护」5 条。

开启总开关时的授权流程：

1. 弹出说明页（Google Play 要求的显眼说明）：读什么、不读什么、不上传。用户按「同意并前往设置」。
2. 用 `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` 跳到系统设置。
3. 回到 App 时检查是否已授权。未授权且 Android 13 以上：显示图文步骤「设置 → 应用 → 本 App → 右上角 ⋮ → 允许受限制的设置」（直接安装的 APK 需要），提供按钮跳到本 App 的应用信息页。
4. 再提示把 App 加入电池优化白名单（`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 或跳到电池设置），并说明小米 / OPPO / vivo 需要另外开「自启动」。

**解析测试页**（只在 debug 版）：粘贴一段通知文字 + 选择 App，显示解析结果和分类结果。用来对着用户给的样本调规则。

### 5.11 待确认

照原型：顶部说明、每笔一张卡（来自哪个 App、时间、商家、建议分类、金额）。有配对账单时加蓝色提示「配对到账单「X」…」；可能重复时加黄色提示「可能和「Y」重复」并默认推荐忽略；转账配对时显示「自己账户之间转账」。点建议分类可以换分类，并有「这个商家每次都问我」勾选。两个按钮：忽略 / 确认入账。全部处理完显示「全部确认完了」。

### 5.12 分类管理 / 新增分类

- 分类管理：支出、收入两组格子；长按弹出菜单：改名、换颜色、加子分类、归档；拖动排序。内置分类不能删除只能归档。有记录的分类归档后历史记录保留。
- 新增分类：大圆球预览、名称（中文最多 4 个字，英文最多 12 个字母）、类型（支出 / 收入）、8 种颜色、（可选）上级分类。同名时提示「这个分类已经有了」并禁止保存。

### 5.13 备份与导出

照原型：上次备份时间、自动备份开关、频率（每天 / 每周）、备份位置（用 SAF 选文件夹，可选 Google Drive）、备份密码、立即备份、导出 Excel / CSV、从备份恢复。

- **备份文件**：`lazee-backup-YYYYMMDD-HHmm.lgb`，内容是 zip（`data.json` = 全部表 + 设置，`photos/`），用 AES-256-GCM 加密，密钥由用户密码经 PBKDF2-HMAC-SHA256（≥ 210,000 次、随机 16 字节 salt）得出；文件头写格式版本号。第一次备份时要求设密码，密码加密后存本机（自动备份要用）。
- 自动备份只保留最新 10 个文件。
- **恢复**：选文件 → 输入密码 → 确认「会取代目前所有资料」→ 恢复前先自动做一次当前资料的备份。
- **导出 CSV**：选日期范围；列 = 日期、类型、分类、子分类、金额、账户、商家、备注、标签、来源；列名跟随当前语言。
- 底部说明：App 本身不上网，存到 Google Drive 时由 Drive App 上传。

### 5.14 外观与语言

照原型：语言两个大按钮（中文 / English），主题颜色 5 行（深色大圆 + 主色小圆 + 名称 + 勾），下方预览卡。切换立即生效，不重启 App。

### 5.15 App 锁

开启后，App 从后台回到前台超过 30 秒就要求指纹 / 手机 PIN。锁住时界面模糊或只显示 Logo。同时对 Activity 设 `FLAG_SECURE`，最近任务里看不到金额。

## 6. 通知自动记账：流程、解析、分类、安全

这是核心功能。每条通知按下面的顺序处理，任何一步判定「不处理」就立刻丢弃，不留任何痕迹。

1. **接收**：`NotificationListenerService.onNotificationPosted`。以下情况直接 return：总开关关闭；包名不在用户勾选的白名单；包名是本 App 自己（这样账单提醒不会被当成付款）；是群组摘要通知（`FLAG_GROUP_SUMMARY`）。只在内存里读 `EXTRA_TITLE`、`EXTRA_TEXT`、`EXTRA_BIG_TEXT`。
2. **验证码过滤**：文字里有 `TAC`、`OTP`、`PIN`、`one-time`、`verification code`、`kod pengesahan`、`验证码`、`动态码`（不分大小写）就丢弃。这一步必须在解析之前。
3. **解析**：按包名找 `assets/parsers/<package>.json`。规则是一组带命名分组的正则（`amount`、`merchant`，可选 `last4`），每条规则标明 `direction`：OUT / IN / OWN\_TRANSFER / REFUND / IGNORE。`ParserEngine` 按顺序试，第一条匹配的生效；都不匹配就丢弃。金额支持 `RM6.80`、`RM 1,234.50`、`MYR 6.80`。
4. **整理商家名**：去掉前后空格、分店编号、尾部的 `SDN BHD` / `ENTERPRISE` / `- KL` 之类，转大写作为 `merchantKey`；显示时用原始大小写。
5. **去重**：`dedupeKey = 包名 + 金额 + merchantKey + 时间(到分钟)`。同一条通知被更新或重发不会产生第二笔。
6. **自己账户之间转账**：解析为 OWN\_TRANSFER（如 reload / top up），或者 10 分钟内两个不同 App 出现同金额的一出一进，就合并成一笔 TRANSFER，不算支出。
7. **和账单 / 手动记录去重**：
   - 配对账单：本月未付的 Bill，金额相差 ≤ RM 1，日期在到期日前 10 天到后 7 天之间 → 写入 `matchedBillId`。确认后写 BillPayment(method = NOTIFICATION)，该账单本月不再提醒。
   - 可能重复：最近 3 天内已经有同金额的手动记录、手动标记已付的账单，或 AUTO\_RECORD 账单自动记的一笔 → 写入 `possibleDuplicateOf`，一律放进待确认，绝不自动入账。
8. **分类**（找到就停）：
   1. 用户的 MerchantRule（商家记忆）→ HIGH
   2. `assets/merchants.json` 内置词库（完整匹配或前缀匹配）→ MEDIUM
   3. `assets/keywords.json` 关键词规则 → MEDIUM
   4. 都没有 → 「其它」，LOW，待确认卡片显示「请选分类」
   - 转账给别人（DuitNow 转账到人名）不猜分类，一律 LOW。
   - REFUND：记成同分类的负支出（减少该分类支出），不算收入。
9. **决定怎么入账**：
   - 懒人「熟悉商家自动入账」开着，且 confidence = HIGH、`confirmCount ≥ 3`、不是 alwaysAsk、没有配对账单、没有可能重复 → 直接建 Transaction(isAuto = true, source = NOTIFICATION)，发一条安静通知「已自动记账 7-Eleven RM 6.80 \[撤销\]」。
   - 其它情况 → 建 PendingTxn。懒人「通知栏直接确认」开着就发通知「7-Eleven RM 6.80 → 购物」，带三个按钮：确认（BroadcastReceiver 直接入账）、改分类（打开待确认页）、忽略。通知用 `VISIBILITY_PRIVATE`，锁屏时不显示金额。
10. **用户确认时**：建 Transaction(source = NOTIFICATION, merchant, isAuto = false)；更新 MerchantRule：同一分类 → confirmCount + 1，换了分类 → 改成新分类并把 confirmCount 重置为 1。账户：按来源 App 对应的账户（用户可在设置里对应，默认按 App 名匹配账户名）。

**安全硬性规定**

- 通知原文只在内存里存在，处理完就丢掉；不写数据库、不写文件、不写 Logcat（release 版把所有含内容的日志去掉）。
- 只存：金额、商家、时间、来源 App、卡号末 4 位（如果有）。
- 没有网络权限，所以物理上传不出去。

**解析规则 JSON 格式（示例，正则是假的，待用户给真实样本后替换）**

```json
{
  "package": "<待确认的包名>",
  "displayName": "TNG eWallet",
  "rules": [
    { "direction": "IGNORE", "pattern": "(?i)reward|promo" },
    { "direction": "OUT", "pattern": "(?i)paid RM\\s?(?<amount>[\\d,]+\\.\\d{2}) to (?<merchant>.+?)\\.?$" },
    { "direction": "OWN_TRANSFER", "pattern": "(?i)reload of RM\\s?(?<amount>[\\d,]+\\.\\d{2})" }
  ]
}
```

**商家词库**（`merchants.json`，至少放这些，键是大写 merchantKey 前缀）：

- 餐饮：MCDONALD, KFC, ZUS, TEALIVE, STARBUCKS, PIZZA HUT, DOMINO, SUBWAY, MARRYBROWN, OLDTOWN, GRABFOOD, FOODPANDA, SHOPEEFOOD
- 交通：SHELL, PETRONAS, PETRON, CALTEX, BHPETROL, GRAB（非 GRABFOOD）, AIRASIA RIDE, RAPID, TOUCH N GO TOLL, PARKING
- 购物：SHOPEE, LAZADA, 7-ELEVEN, 99 SPEEDMART, KK MART, FAMILYMART, MYDIN, LOTUS, AEON, GIANT, MR DIY, WATSONS, GUARDIAN, UNIQLO
- 账单：TNB, UNIFI, TM, CELCOMDIGI, DIGI, MAXIS, HOTLINK, UMOBILE, YES, AIR SELANGOR, INDAH WATER
- 娱乐：NETFLIX, SPOTIFY, YOUTUBE, DISNEY, GSC, TGV, MBO, STEAM

**关键词规则**（`keywords.json`）：RESTORAN / KOPITIAM / CAFE / KEDAI MAKAN / NASI / MEE / BAKERY → 餐饮；PARKING / TOLL / PETROL → 交通；PRINTING / KEDAI BUKU / BOOKSTORE / STATIONERY → 教育；CLINIC / KLINIK / FARMASI / PHARMACY → 其它。

## 7. 后台任务与系统通知

全部用 WorkManager，不设网络约束。时间都是手机本地时间。

| Worker | 什么时候跑 | 做什么 |
| --- | --- | --- |
| BillReminderWorker | 每天 09:00 | 对本月未付、且今天 = 到期日 − remindDaysBefore 或今天 = 到期日的账单发提醒，按钮「已付款」「明天再提醒」。mode = AUTO\_RECORD 的账单在到期日自动记一笔（isAuto = true）并写 BillPayment(AUTO)。分期付完最后一期后把账单设为不启用。 |
| DailySummaryWorker | 每天，用户设定的时间（默认 21:00） | 「今天记了 N 笔、花了 RM x，还有 M 笔待确认」。今天一笔都没有时改成「今天还没记账哦」。 |
| MonthlySummaryWorker | 每月 1 日 09:00 | 「10 月花了 RM x，存下 y%，花最多的是餐饮」，点开进上月统计。 |
| AutoBackupWorker | 每天或每周 03:00（设 `requiresBatteryNotLow`） | 写加密备份到用户选的文件夹，只留最新 10 个。文件夹失效时发通知请用户重选。 |

预算提醒不是 Worker：每次新增支出后立即检查（见 5.3）。

**通知渠道**（名称要翻译）：自动记账（默认重要性 DEFAULT）、账单提醒（HIGH）、预算提醒（DEFAULT）、每日 / 每月汇总（LOW）、备份（LOW）。所有带金额的通知都用 `VISIBILITY_PRIVATE` 并提供不含金额的 publicVersion。

有 BootReceiver：开机后重新排好各个周期任务（WorkManager 一般会自己恢复，这里只是保险）。

## 8. 安全与隐私清单

发布前逐项检查：

- [ ] `AndroidManifest.xml` 里**没有** `INTERNET` 和 `ACCESS_NETWORK_STATE`（合并后的 manifest 也要查，确保没有任何库偷偷加进来）。
- [ ] 数据库用 SQLCipher 加密；密码随机生成 32 字节，用 Keystore 里的 AES-GCM 密钥加密后存放。
- [ ] 通知原文不落盘、不进日志；验证码通知在解析前就被丢弃。
- [ ] 只处理白名单 App；忽略本 App 自己的通知。
- [ ] 卡号只保留末 4 位。
- [ ] `android:allowBackup="false"`，并设 `dataExtractionRules` 把所有内容排除在云备份和设备转移之外（用户要用我们自己的加密备份）。
- [ ] 备份文件 AES-256-GCM + PBKDF2 加密，密码错误时明确提示、不崩溃。
- [ ] App 锁开启时设 `FLAG_SECURE`。
- [ ] 通知在锁屏上不显示金额。
- [ ] release 版开启 R8 混淆 + 资源压缩，并保留 Room / kotlinx.serialization / SQLCipher 需要的 keep 规则。
- [ ] 「关于 → 隐私说明」写清楚：读什么、存什么、不上网、怎么关闭。

## 9. 设计规范：颜色、主题、字体、组件

所有数值来自原型。只有浅色模式（V1 不做深色模式）。

**不随主题变的基础色**

| 用途 | 颜色 |
| --- | --- |
| 页面背景 | #F5F6F8 |
| 卡片 | #FFFFFF |
| 主文字 | #14161A |
| 次文字 | #5F6672 |
| 分隔线 | #EEF0F3 |
| 边框 | #DADDE3 |
| 关闭的开关 / 不可用按钮 | #C9CDD4 / #A9B0BA |
| 警告底 / 字 | #FFF3DC / #6E4400（强调字 #8A5300） |
| 信息底 / 字 | #E6F0FB / #1D4E89 |
| 危险字 / 条 | #B42318 / #D92D20 |
| 进度 ≥ 80% | #E39A1E |

**5 个主题**（字段含义：accent = 按钮、开关、选中；deep = 首页大卡等深色块；tint / tintText = 浅色提示底和字；glow = 深色块上的进度条；sub = 深色块上的次文字；soft = 收入柱等浅色图形）

| 主题 | accent | deep | tint | tintText | glow | sub | soft | 热力图 4 档 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 森林绿 Forest（默认） | #0E7C66 | #12352E | #E3F4EE | #0A5C4C | #7EE0C3 | #B9D6CE | #B7DDD3 | #CDE8E0 #8CCBB8 #3F9C85 #12352E |
| 海湾蓝 Ocean | #2457C5 | #142A55 | #E6EEFB | #1D428F | #9DBDF5 | #B8C8E8 | #B3C8EF | #D6E2F7 #A9C1EE #6E95DE #142A55 |
| 陶土橙 Terracotta | #B4441A | #4A1F0F | #FBEDE6 | #8F3412 | #F5B596 | #E5C3B3 | #EDB196 | #F8E0D4 #F0BCA3 #E08A63 #4A1F0F |
| 葡萄紫 Grape | #6941C6 | #2A1B54 | #F0EBFB | #53359E | #C4B1F2 | #CEC3EA | #BFAEEC | #E6DEF8 #C9B8F0 #A189E0 #2A1B54 |
| 墨黑 Ink | #14161A | #14161A | #EEF0F3 | #14161A | #E6E8EC | #A9B0BA | #C9CDD4 | #E6E8EC #C9CDD4 #9AA0A8 #14161A |

分类占比条的 7 级色阶见 `docs/prototype.html` 里的 `THEMES[*].chart`。

**分类颜色**（8 种，不随主题变，底色 / 字色）：0 橙 #FDECE3 / #9A3B12、1 蓝 #E6F0FB / #1D4E89、2 紫 #F1ECFB / #5B3A9E、3 粉 #FCE8F0 / #9B2459、4 绿 #E3F4EE / #0A5C4C、5 黄 #FFF6DD / #7A5A00、6 红 #FBEAEA / #9B2C2C、7 灰 #EEF0F3 / #3F4752。

**字体与字号**：拉丁字母和数字用 Inter，中文用 Noto Sans SC。页面标题 24 Bold；首页大月份 44 Bold（字距 −1.5）；大卡金额 34 Bold；记账页金额 38 Bold；区块标题 15 SemiBold；正文 / 列表 15；辅助字 12–13；导航文字 11。

**形状与间距**：页面左右内边距 20；区块间距 14–16；大卡圆角 20、列表卡 16–18、按钮 12–14、小标签 8、胶囊按钮全圆；开关 52×32；所有可点元素 ≥ 44dp。

**共用组件**（放 `core/ui/components`）：CategoryBadge（圆形，分类名首字，英文用大写首字母）、SettingRow（标题 + 描述 + 开关 / 箭头 / 状态标签）、SegmentedControl、StatusTag、SectionCard、HeroCard、InfoBanner（警告 / 信息 / 主题三种）、AmountText（处理隐藏金额）、CalcKeypad、Toast。

**无障碍**：文字对比度 ≥ 4.5:1；只有图标的按钮要有 `contentDescription`；支持系统字体放大到 130% 不破版。

**App 图标（已定：懒记的「睡着的金币」）**：薄荷绿底 #7EE0C3 上一枚深绿 #12352E 的圆形金币，金币上两只闭着的眼睛和一个小微笑（薄荷绿线条），右上角两个「z」。做成自适应图标：

- `ic_launcher_background`：纯色 #7EE0C3。
- `ic_launcher_foreground`：把下面 SVG 去掉背景矩形后转成 VectorDrawable，内容缩到安全区内（108dp 画布的中间 66dp）。
- `ic_launcher_monochrome`：只用金币轮廓 + 眼睛 + z 的单色版本，给 Android 13 主题图标用。
- 引导页和 App 锁界面也用这个图标。

```svg
<svg viewBox="0 0 160 160" xmlns="http://www.w3.org/2000/svg">
  <rect width="160" height="160" rx="40" fill="#7EE0C3"/>
  <circle cx="78" cy="88" r="44" fill="#12352E"/>
  <path d="M58 84q8 9 16 0M84 84q8 9 16 0" fill="none" stroke="#7EE0C3" stroke-width="6" stroke-linecap="round"/>
  <path d="M70 104q8 5 16 0" fill="none" stroke="#7EE0C3" stroke-width="5" stroke-linecap="round"/>
  <path d="M110 28h16l-16 16h16" fill="none" stroke="#12352E" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M126 52h9l-9 9h9" fill="none" stroke="#12352E" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
```

通知栏小图标（`ic_stat_lazee`）：白色单色的金币 + 闭眼，不要背景。

## 10. 多语言

- `res/values/strings.xml` = 英文（默认），`res/values-zh/strings.xml` = 简体中文。所有文案对照原型里 `L('中文', 'English')` 的写法。
- App 内切换用「每个 App 单独设语言」：`AppCompatDelegate.setApplicationLocales(...)` + `res/xml/locales_config.xml`（manifest 里声明）。第一次启动默认跟随手机语言：手机是中文就中文，其它一律英文。
- 内置分类名用 builtInKey 取字符串资源；用户自建的分类、账户、账单名称显示原样。
- 金额永远是 `RM 1,234.50`（千分位逗号、两位小数），不跟语言变。
- 日期：中文「11 月 12 日 · 星期四」、英文「12 Nov · Thu」；分组标题用「今天 / 昨天 / Today / Yesterday」。周一作为一周第一天。
- 英文句子比中文长，所有文字容器都要允许换行，不准裁掉。

## 11. 构建与发布：GitHub Actions 出 APK

用户的电脑不跑 Android Studio，所有编译都在 GitHub Actions 里做。

**工作流 `.github/workflows/android.yml`**

1. 触发：push 到 `main`、pull request、手动 `workflow_dispatch`。
2. 步骤：`actions/checkout` → `actions/setup-java`（temurin 17）→ `gradle/actions/setup-gradle` → `./gradlew testDebugUnitTest assembleDebug`。
3. 用 `actions/upload-artifact` 上传 APK（保留 14 天）。
4. **只在 push 到 main 时**：用 `softprops/action-gh-release` 把 APK 发布到 tag 为 `build-<run_number>` 的 Release（prerelease），文件名 `lazee-v<versionName>-<run_number>.apk`。用户在手机上打开仓库的 Releases 页就能下载，不用登录 Actions。只保留最新 5 个这类 Release。
5. `versionCode` = `GITHUB_RUN_NUMBER`（本地构建时用 1），`versionName` = `1.0.0`。

**签名（重要）**：每次编译出的 APK 必须用同一把钥匙签名，否则新版本装不进旧版本上，用户就得删掉 App、丢掉资料。

- 生成一把测试用的 keystore，用 base64 存入 GitHub Secrets：`SIGNING_KEYSTORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。工作流里解码成文件，`debug` 构建类型用它签名。
- 用 `gh secret set` 设置这些 secret。如果没有权限设置，就在 README 里写清楚让用户自己在网页上设；secret 不存在时工作流退回用默认 debug 签名，并在 Release 说明里写「这个版本要先删掉旧版才能装」。
- 绝不把 keystore 文件或密码 commit 进仓库。上架 Google Play 时再另建正式钥匙（不在 V1 范围）。

**Gradle wrapper**：必须提交 `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 和 `.properties`。本地如果生成不了 wrapper（没网络），就在工作流里加一个一次性的步骤用 `gradle wrapper` 生成，或用 `setup-gradle` 的 `gradle-version` 直接跑 `gradle`，并在 `docs/DECISIONS.md` 记下。

**大小**：APK 目标 < 25 MB（主要是中文字体）。太大时可用 `abiFilters` 只留 `arm64-v8a` 和 `armeabi-v7a`（SQLCipher 有原生库）。

## 12. 测试要求

单元测试必须覆盖下面这些纯逻辑，CI 每次都跑，不过就不出 APK：

- **计算器**：`12+8.5 = 20.50`、先乘除后加减、连按运算符、小数位限制、除以 0、四舍五入。
- **Money**：分 ↔ `RM 1,234.50` 格式化与解析。
- **验证码过滤**：含 TAC / OTP / 验证码 / kod pengesahan 的文字全部被丢弃。
- **ParserEngine**：`src/test/resources/notifications/<app>/*.txt` 里每个样本配一个同名 `.expected.json`（amount、merchant、direction），测试逐个比对。样本必须是遮蔽过的。先放自己编的假样本，用户给真样本后替换。
- **分类器**：优先顺序（商家记忆 > 词库 > 关键词 > 其它）、GRAB vs GRABFOOD、转给人名不猜。
- **去重与配对**：同一通知两次只产生一笔；MAE 充值 TNG 合并成转账；账单金额 ±RM 1 和日期窗口；手动标记已付后再来的通知被标「可能重复」。
- **懒人自动入账条件**：confirmCount 不到 3、alwaysAsk、有配对、有重复时都不能自动入账。
- **预算与统计**：每天可花、月底预测、储蓄率、无消费日、周末 vs 平日、转账不算收支、退款减少支出。
- **账单日期**：31 号遇到 2 月 / 30 天的月、分期第几期。
- **借款利息**：单利公式。
- **备份**：加密 → 解密还原一致；错密码抛出明确异常。
- **CSV**：中文和逗号、引号正确转义。
- **Room**：用 Robolectric 测 DAO 的主要查询（按月汇总、按分类汇总、商家 Top 5）。

UI 测试不强制。用户会用真机做手动测试。

## 13. 开发顺序与验收清单

V1 一次做完，但按下面 7 个里程碑依次做。每个里程碑结束都要 push 并确认 GitHub Actions 变绿，这样用户随时能拿到一个能装的 APK。

1. **M1 骨架**：Gradle 项目、version catalog、Hilt、Room + SQLCipher、DataStore、主题（5 套）、字体、底部导航空页面、GitHub Actions（含签名和 Release）、`CLAUDE.md`、`docs/`。
2. **M2 记账核心**：引导页、账户、分类（含二级、新增、管理）、记一笔（计算器、日期、照片、标签、模板）、首页明细、交易详情、批量操作、搜索筛选、隐藏金额。
3. **M3 预算与统计**：总预算 / 分类预算 / 超支提醒 / 每天可花、存钱目标、统计页全部 8 块、分类明细、年度回顾。
4. **M4 通知自动记账**：Listener、验证码过滤、ParserEngine + JSON 规则、分类器 + 词库、去重 / 转账配对、待确认页、通知按钮、懒人模式、授权引导、debug 解析测试页。
5. **M5 账单与借还**：账单提醒（含自动记账、订阅、分期）、和通知记账的配对、信用卡账单日、借还款、净资产、所有 Worker。
6. **M6 备份与安全**：加密备份 / 恢复 / 自动备份、CSV 导出、App 锁、`FLAG_SECURE`、第 8 节清单。
7. **M7 收尾**：中英文文案全部核对、外观与语言页、关于 / 隐私说明、空状态和错误提示、README、验收清单打勾。

**验收清单**（全部打勾才算 V1 完成）

- [ ] GitHub Actions 变绿，Releases 页有可下载的 APK，新版能直接覆盖安装旧版。
- [ ] 合并后的 manifest 里没有任何网络权限。
- [ ] 记一笔：计算器、日期切换、新增分类、二级分类、照片、`#标签`、模板、转账都能用。
- [ ] 首页、统计、预算、我的与原型一致，5 个主题切换即时生效。
- [ ] 中文 / English 切换后没有任何写死的文字。
- [ ] 统计页 8 块都用真实资料计算（没有假数字）。
- [ ] 用 debug 解析测试页输入样本，能解析出金额和商家；验证码样本被丢弃。
- [ ] 真机上发一条白名单 App 的付款通知后，出现待确认和带按钮的通知，按「确认」不用打开 App 就入账。
- [ ] 同一商家确认 3 次后，第 4 次自动入账并标「自动」，可撤销。
- [ ] 信用卡还款通知自动配对账单并停止提醒；手动标记已付后再来的通知不会自动重复入账。
- [ ] 账单提醒、每晚汇总、月度总结、超支提醒按时出现。
- [ ] 备份到文件夹 → 卸载重装 → 恢复后资料和照片完整；错密码有提示。
- [ ] 导出的 CSV 用 Excel 打开中文不乱码。
- [ ] App 锁生效，最近任务里看不到金额。
- [ ] 所有单元测试通过。
- [ ] README 写好安装和开启通知权限的步骤。

## 14. 待用户提供 / 待决定

这些不影响开工，先用占位，拿到后替换。

- [ ] **GitHub 仓库**：用户建好私人仓库并连接到 Claude（建议仓库名 lazee）。
- [x] **App 名称和 Logo**：已定：懒记 Lazee（方案 C，睡着的金币）。M1 就要：改 `app_name`、替换 adaptive icon、更新 applicationId、备份文件名前缀。先在 Google Play 搜一下确认名字没有被用掉。
- [ ] **2–3 个 App 的付款通知截图**（卡号、余额遮掉），包括：普通付款、充值 / 转自己账户、转给别人、退款、信用卡还款。拿到后按样本写 `parsers/*.json` 和测试样本，并确认各 App 的包名。在那之前，白名单里只显示「即将支持」。
- [ ] **Google Play 上架**：不在 V1 范围。以后要上架时再处理开发者账号、正式签名、隐私政策网页和通知权限的审核说明。

**V1 不做**：深色模式、桌面小组件、多人共享账本、多币种、语音 / 拍照识别、银行直连、任何云端服务。
