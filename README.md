# 懒记 Lazee

给懒人用的离线记账 Android App（马来西亚，RM）：读取银行 / 电子钱包的付款通知自动记账，你睡觉它也在帮你记。完全不上网，资料只存在你的手机里，并且加密。

- 开发规格书：[docs/SPEC.md](docs/SPEC.md)
- 界面原型：[docs/prototype.html](docs/prototype.html)
- 开发中的取舍记录：[docs/DECISIONS.md](docs/DECISIONS.md)

## 下载 APK

1. 用手机打开本仓库的 **Releases** 页面（仓库首页右边「Releases」）。
2. 点最新的「Lazee build N」，下载 `lazee-v1.0.0-N.apk`。

> 每次推送到 `main`，GitHub Actions 会自动编译、跑完所有测试，再把 APK 发布到 Releases（只保留最新 5 个）。
> 如果 Actions 跑不起来（例如账户被锁），也可以从 Actions 运行记录的「Artifacts」下载，或请开发者直接发 APK。

## 安装

1. 在手机上点下载好的 APK。
2. 如果提示「不允许安装未知应用」，按提示进入设置，允许你的浏览器 / 文件管理器「安装未知应用」，再回来点一次。
3. 以后的新版本直接安装就会覆盖旧版本，资料不会丢（前提是用同一把签名钥匙，见下面「签名」）。

## 开启通知自动记账

1. 打开懒记 → 「我的」→「通知自动记账」→ 打开「开启自动记账」。
2. 看完说明后按「同意并前往设置」，在系统的「通知使用权」里打开 **懒记**，然后返回。
3. **Android 13 以上**：如果开关是灰色的，或提示「受限制的设置」（直接安装的 APK 都会这样）：
   1. 设置 → 应用 → 懒记
   2. 点右上角 **⋮** → **允许受限制的设置**
   3. 回到第 2 步再打开一次通知使用权
4. 回到懒记，按提示**关闭电池优化**，避免系统在后台把它关掉。
5. **小米 / OPPO / vivo**：还要在系统自带的「安全中心 / 手机管家」里为懒记打开 **自启动**。
6. 在「只读取这些 App 的通知」里勾选你用的 App（目前 TNG eWallet、MAE 标着「即将支持」，规则是根据示例写的，提供真实样本后会更准）。

## 签名（重要，只需设一次）

新版本必须和旧版本用同一把钥匙签名，否则装不上、只能删掉重装（会丢资料）。钥匙文件和密码已经单独交给仓库主人，**不要放进仓库**。

在 GitHub 网页上：仓库 → Settings → Secrets and variables → Actions → New repository secret，加这 4 个：

| 名称 | 内容 |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `SIGNING_KEYSTORE_BASE64.txt` 文件里的全部内容 |
| `SIGNING_STORE_PASSWORD` | `passwords.txt` 里的 `SIGNING_STORE_PASSWORD` |
| `SIGNING_KEY_ALIAS` | `lazee` |
| `SIGNING_KEY_PASSWORD` | `passwords.txt` 里的 `SIGNING_KEY_PASSWORD` |

没设这些 secret 时，工作流会用临时的 debug 钥匙签名，并在 Release 说明里提醒「要先删掉旧版才能装」。

## 自己编译

```bash
./gradlew testDebugUnitTest assembleDebug
```

需要 JDK 17 和 Android SDK（compileSdk 37）。APK 在 `app/build/outputs/apk/debug/`。截图测试会把每个画面存到 `app/build/screenshots/`。

## V1 验收清单

自动化测试共 64 个（逻辑单元测试 + 用 Robolectric 跑真实 App 的端到端测试与截图），全部通过。需要真机才能确认的项目单独标出。

- [x] GitHub Actions 工作流、Releases 发布、固定签名覆盖安装 —— 工作流已写好；仓库主人的 GitHub 账户目前计费被锁，Actions 无法启动，解锁并设好签名 secret 后即可（本地已用固定钥匙编译出可覆盖安装的 APK）
- [x] 合并后的 manifest 没有任何网络权限（CI 自动检查；本地 `aapt2 dump permissions` 已确认）
- [x] 记一笔：计算器、日期切换、新增分类、二级分类、照片、`#标签`、模板、转账
- [x] 首页、统计、预算、我的与原型一致，5 个主题即时切换（截图测试）
- [x] 中文 / English 切换后没有写死的文字（英文截图测试）
- [x] 统计页 8 块都用真实资料计算
- [x] debug 解析测试页能解析金额和商家；验证码样本被丢弃
- [ ] 真机：白名单 App 付款通知 → 待确认 + 带按钮的通知，按「确认」不打开 App 就入账（逻辑已测试，待真机和真实通知样本验证）
- [x] 同一商家确认 3 次后，第 4 次自动入账并标「自动」，可撤销（端到端测试）
- [x] 信用卡 / 账单还款通知自动配对并停止提醒；手动标记已付后再来的通知不会自动重复入账（端到端测试）
- [ ] 真机：账单提醒、每晚汇总、月度总结、超支提醒按时出现（Worker 已实现，待真机观察）
- [x] 备份 → 清空 → 恢复后资料和照片完整；错密码有明确提示（端到端测试）
- [ ] 真机：导出的 CSV 用 Excel 打开中文不乱码（已带 UTF-8 BOM，测试已验证，待用 Excel 打开确认）
- [x] App 锁、`FLAG_SECURE`（最近任务里看不到金额）
- [x] 所有单元测试通过
- [x] README 写好安装和开启通知权限的步骤

## 安全与隐私（规格书第 8 节）

- [x] 没有 `INTERNET` / `ACCESS_NETWORK_STATE`，也没有广告、统计、崩溃上报
- [x] 资料库用 SQLCipher 加密，32 字节随机密码由 Android Keystore 的 AES-GCM 钥匙包住
- [x] 通知原文不落盘、不进日志；验证码通知在解析前就丢弃
- [x] 只处理勾选的 App，忽略懒记自己的通知；卡号只留末 4 位
- [x] `allowBackup="false"` + `dataExtractionRules` 排除所有云备份和设备转移
- [x] 备份 AES-256-GCM + PBKDF2（210,000 次），错密码明确提示
- [x] App 锁开启时 `FLAG_SECURE`；锁屏通知不显示金额
- [x] release 版 R8 混淆 + 资源压缩（release APK 约 6 MB）
- [x] 「关于 → 隐私说明」写清楚读什么、存什么、不上网、怎么关闭
