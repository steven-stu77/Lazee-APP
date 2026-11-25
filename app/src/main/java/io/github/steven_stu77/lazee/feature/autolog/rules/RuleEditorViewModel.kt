package io.github.steven_stu77.lazee.feature.autolog.rules

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.Direction
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserEngine
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserRule
import io.github.steven_stu77.lazee.feature.autolog.parser.Parsed
import io.github.steven_stu77.lazee.feature.autolog.parser.RuleBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppChoice(val pkg: String, val name: String)

data class RuleEditorUiState(
    val apps: List<AppChoice> = emptyList(),
    val pkg: String = "",
    val appName: String = "",
    val sample: String = "",
    val amountText: String = "",
    val merchantText: String = "",
    val direction: Direction = Direction.OUT,
    val incoming: Boolean = true,
    val ignoreNumbers: Boolean = true,
    val advanced: Boolean = false,
    val pattern: String = "",
    val error: RuleBuilder.Reason? = null,
    val badPattern: Boolean = false,
    val testText: String = "",
    val saved: Boolean = false,
    /** Index of the rule being edited inside this app's user rules, or -1 for a new one. */
    val editIndex: Int = -1,
) {
    val needsMerchant: Boolean get() = direction != Direction.IGNORE && direction != Direction.OWN_TRANSFER
    val canSave: Boolean get() = pkg.isNotBlank() && pattern.isNotBlank() && !badPattern && (direction == Direction.IGNORE || error == null)
}

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val config: AutoLogConfig,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RuleEditorUiState())
    val state: StateFlow<RuleEditorUiState> = _state.asStateFlow()

    init {
        val pkg = handle.get<String>("pkg").orEmpty()
        val index = handle.get<Int>("index") ?: -1
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { launcherApps() }
            val existing = config.custom.value.firstOrNull { it.`package` == pkg }?.rules?.getOrNull(index)
            _state.update {
                it.copy(
                    apps = apps, pkg = pkg, appName = apps.firstOrNull { a -> a.pkg == pkg }?.name ?: config.engine.config(pkg)?.displayName ?: pkg,
                    editIndex = if (existing != null) index else -1,
                    sample = existing?.sample.orEmpty(), direction = existing?.direction ?: Direction.OUT,
                    incoming = existing?.incoming ?: true, pattern = existing?.pattern.orEmpty(), advanced = existing != null && existing.sample == null,
                )
            }
        }
    }

    /** Apps with a launcher icon, supported apps first. Needs the <queries> entry in the manifest. */
    private fun launcherApps(): List<AppChoice> {
        val pm = context.packageManager
        val supported = config.parserConfigs.map { it.`package` }.toSet()
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { AppChoice(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedWith(compareBy<AppChoice> { it.pkg !in supported }.thenBy { it.name.lowercase() })
    }

    fun pickApp(a: AppChoice) = _state.update { it.copy(pkg = a.pkg, appName = a.name) }

    fun setSample(text: String) = _state.update { s ->
        val amount = if (s.amountText.isBlank() || !text.contains(s.amountText)) RuleBuilder.detectAmount(text).orEmpty() else s.amountText
        val merchant = if (s.merchantText.isBlank() || !text.contains(s.merchantText)) RuleBuilder.detectMerchant(text).orEmpty() else s.merchantText
        rebuild(s.copy(sample = text, amountText = amount, merchantText = merchant))
    }

    fun setAmount(v: String) = _state.update { rebuild(it.copy(amountText = v)) }
    fun setMerchant(v: String) = _state.update { rebuild(it.copy(merchantText = v)) }
    fun setDirection(d: Direction) = _state.update { rebuild(it.copy(direction = d)) }
    fun setIncoming(v: Boolean) = _state.update { it.copy(incoming = v) }
    fun setIgnoreNumbers(v: Boolean) = _state.update { rebuild(it.copy(ignoreNumbers = v)) }
    fun setAdvanced(v: Boolean) = _state.update { it.copy(advanced = v) }
    fun setPattern(p: String) = _state.update { it.copy(pattern = p, badPattern = runCatching { Regex(p) }.isFailure) }
    fun setTestText(t: String) = _state.update { it.copy(testText = t) }

    /** In simple mode the pattern always follows the sample and the marked parts. */
    private fun rebuild(s: RuleEditorUiState): RuleEditorUiState {
        if (s.advanced || s.sample.isBlank()) return s
        val merchant = if (s.needsMerchant) s.merchantText else null
        return when (val r = RuleBuilder.build(s.sample, s.amountText, merchant, s.ignoreNumbers)) {
            is RuleBuilder.Result.Ok -> s.copy(pattern = r.pattern, error = null, badPattern = false)
            is RuleBuilder.Result.Error -> if (s.direction == Direction.IGNORE) s.copy(pattern = ignorePattern(s.sample), error = null) else s.copy(error = r.reason)
        }
    }

    /** "Ignore this kind" needs no amount: match the first few words of the sample. */
    private fun ignorePattern(sample: String): String =
        "(?i)" + sample.trim().split(Regex("\\s+")).take(4).joinToString("\\s+") { w -> w.filter { it.isLetter() }.ifEmpty { "\\S+" } }

    private fun rule(s: RuleEditorUiState) = ParserRule(
        direction = s.direction, pattern = s.pattern, sample = s.sample.ifBlank { null },
        incoming = if (s.direction == Direction.OWN_TRANSFER) s.incoming else null,
    )

    sealed class Test {
        data class Match(val parsed: Parsed) : Test()
        data object Ignored : Test()
        data object NoMatch : Test()
        data object Empty : Test()
    }

    /** Runs the rule as it would run on a real notification. */
    fun test(text: String): Test {
        val s = _state.value
        if (text.isBlank()) return Test.Empty
        if (s.pattern.isBlank() || s.badPattern) return Test.NoMatch
        val regex = runCatching { Regex(s.pattern) }.getOrNull() ?: return Test.NoMatch
        val flat = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        if (s.direction == Direction.IGNORE) return if (regex.containsMatchIn(flat)) Test.Ignored else Test.NoMatch
        val pkg = s.pkg.ifBlank { "x" }
        val parsed = runCatching { ParserEngine(listOf(ParserConfig(pkg, s.appName.ifBlank { "App" }, rules = listOf(rule(s))))).parse(pkg, text) }.getOrNull()
        return if (parsed != null) Test.Match(parsed) else Test.NoMatch
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val all = config.custom.value.toMutableList()
            val i = all.indexOfFirst { it.`package` == s.pkg }
            val app = if (i >= 0) all[i] else ParserConfig(s.pkg, s.appName, rules = emptyList(), custom = true)
            val rules = app.rules.toMutableList().apply { if (s.editIndex in indices) set(s.editIndex, rule(s)) else add(0, rule(s)) }
            val updated = app.copy(rules = rules, displayName = s.appName.ifBlank { app.displayName })
            if (i >= 0) all[i] = updated else all.add(updated)
            withContext(Dispatchers.IO) { config.saveCustom(all) }
            // A new app is switched on right away so the rule starts working.
            val enabled = settings.current().enabledPackages
            if (s.pkg !in enabled) settings.setEnabledPackages(enabled + s.pkg)
            _state.update { it.copy(saved = true) }
        }
    }
}
