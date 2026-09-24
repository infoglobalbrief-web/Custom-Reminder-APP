# Remindly compile-fixes patcher - ASCII only (safe for Windows PowerShell 5.1)
# Save as apply-fixes.ps1 in E:\Reminder-APP\Custom-Reminder-APP
# Run:  powershell -ExecutionPolicy Bypass -File .\apply-fixes.ps1
$ErrorActionPreference = 'Stop'
$root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$base = Join-Path $root 'app\src\main\java\com\remindly\app'
if (-not (Test-Path $base)) {
  Write-Host 'ERROR: app\src not found. Run from Custom-Reminder-APP root.'
  exit 1
}

function Patch($rel, $old, $new) {
  $path = Join-Path $base ($rel -replace '[\\/]', [IO.Path]::DirectorySeparatorChar)
  if (-not (Test-Path $path)) { Write-Host "[skip] missing $rel"; return }
  $text = [IO.File]::ReadAllText($path)
  if ($text.Contains($new)) { Write-Host "[ok]   $rel"; return }
  if (-not $text.Contains($old)) { Write-Host "[MISS] $rel"; return }
  [IO.File]::WriteAllText($path, $text.Replace($old, $new))
  Write-Host "[FIX]  $rel"
}

# ---- 1) PersonEntity: add updatedAt (after createdAt, before deletedAt) ----
$rel = 'core\data\entity\Entities.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if ($t.Contains('val updatedAt')) { Write-Host "[ok]   $rel" }
  else {
    $pattern = 'val createdAt: Long = System.currentTimeMillis(),'
    # Only inside PersonEntity: second occurrence (ReminderEntity already has updatedAt)
    $idx = $t.LastIndexOf($pattern)
    if ($idx -ge 0) {
      $insert = "val createdAt: Long = System.currentTimeMillis(),`r`n    val updatedAt: Long = System.currentTimeMillis(),"
      # use whatever newline the file has
      if ($t.Contains("`n") -and -not $t.Contains("`r`n")) { $insert = "val createdAt: Long = System.currentTimeMillis(),`n    val updatedAt: Long = System.currentTimeMillis()," }
      else { $insert = "val createdAt: Long = System.currentTimeMillis(),`r`n    val updatedAt: Long = System.currentTimeMillis()," }
      $t = $t.Substring(0, $idx) + $insert + $t.Substring($idx + $pattern.Length)
      [IO.File]::WriteAllText($path, $t)
      Write-Host "[FIX]  $rel (updatedAt added)"
    } else { Write-Host "[MISS] $rel createdAt" }
  }
}

# ---- 2) Scheduler: toMinuteOfDay does not exist ----
Patch 'core\domain\scheduler\ReminderScheduler.kt' `
  'h = h * 31 + at.toLocalTime().toMinuteOfDay()' `
  'h = h * 31 + (at.hour * 60 + at.minute)'

# ---- 3) NotificationHelper: remove NAGGING block ----
$rel = 'core\notifications\NotificationHelper.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if (-not $t.Contains('NAGGING')) { Write-Host "[ok]   $rel" }
  else {
    $t = $t -replace '(?s)fun areNotificationsEnabled\(\): Boolean \{.*?NAGGING.*?\}', 'fun areNotificationsEnabled(): Boolean ='
    # simplify: drop the if-block lines only
    $t = [IO.File]::ReadAllText($path)
    $t = $t -replace '(?m)^\s*if \(Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.NAGGING\) \{\r?\n', ''
    $t = $t -replace '(?m)^\s*// areNotificationsEnabled exists via NotificationManagerCompat\r?\n', ''
    $t = $t -replace '(?m)^\s*\}\r?\n(\s*return androidx\.core\.app\.NotificationManagerCompat)', "`$1"
    # convert multi-line fun to expression body if still present
    if ($t.Contains('NAGGING')) {
      $t = $t -replace '(?s)fun areNotificationsEnabled\(\): Boolean \{\s*return (androidx\.core\.app\.NotificationManagerCompat\.from\(context\)\.areNotificationsEnabled\(\))\s*\}', 'fun areNotificationsEnabled(): Boolean = `$1'
    }
    [IO.File]::WriteAllText($path, $t)
    if ($t.Contains('NAGGING')) { Write-Host "[MISS] $rel still has NAGGING - manual delete needed" }
    else { Write-Host "[FIX]  $rel" }
  }
}

# ---- 4) CalendarScreen: remove WeekFields junk lines ----
$rel = 'features\calendar\CalendarScreen.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t0 = [IO.File]::ReadAllText($path)
  $t = $t0
  $t = $t -replace '(?m)^\s*val weekFields = WeekFields\.of[^\r\n]*\r?\n', ''
  $t = $t -replace '(?m)^\s*weekFields\.dayOfWeek\.minimalDaysInFirstWeek\r?\n', ''
  $t = $t -replace '(?m)^import java\.time\.temporal\.WeekFields\r?\n', ''
  if ($t -ne $t0) { [IO.File]::WriteAllText($path, $t); Write-Host "[FIX]  $rel" }
  else { Write-Host "[ok]   $rel" }
}

# ---- 5) Onboarding: OptIn for HorizontalPager ----
$rel = 'features\onboarding\OnboardingScreen.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if ($t.Contains('ExperimentalFoundationApi')) { Write-Host "[ok]   $rel" }
  else {
    $nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }
    $needle = "@Composable${nl}fun OnboardingScreen"
    $repl   = "@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)${nl}@Composable${nl}fun OnboardingScreen"
    if ($t.Contains($needle)) {
      [IO.File]::WriteAllText($path, $t.Replace($needle, $repl))
      Write-Host "[FIX]  $rel"
    } else { Write-Host "[MISS] $rel" }
  }
}

# ---- 6) PeopleScreens: width import ----
Patch 'features\people\PeopleScreens.kt' `
  "import androidx.compose.foundation.layout.padding`nimport androidx.compose.foundation.lazy.LazyColumn" `
  "import androidx.compose.foundation.layout.padding`nimport androidx.compose.foundation.layout.width`nimport androidx.compose.foundation.lazy.LazyColumn"

# ---- 7) QuickAddSheet: clickable import + call ----
Patch 'features\reminders\QuickAddSheet.kt' `
  "import androidx.compose.foundation.background`nimport androidx.compose.foundation.layout.Arrangement" `
  "import androidx.compose.foundation.background`nimport androidx.compose.foundation.clickable`nimport androidx.compose.foundation.layout.Arrangement"
Patch 'features\reminders\QuickAddSheet.kt' `
  'androidx.compose.foundation.clickable(' `
  'clickable('

# ---- 8) ReminderDetailScreen: Flow.first ----
Patch 'features\reminders\ReminderDetailScreen.kt' `
  'import com.remindly.app.core.domain.model.ReminderStatus' `
  "import com.remindly.app.core.domain.model.ReminderStatus`nimport kotlinx.coroutines.flow.first"
Patch 'features\reminders\ReminderDetailScreen.kt' `
  'else kotlinx.coroutines.flow.first(container.reminderRepository.search(query))' `
  'else container.reminderRepository.search(query).first()'

# ---- 9) TasksScreen: fillMaxWidth import ----
Patch 'features\tasks\TasksScreen.kt' `
  "import androidx.compose.foundation.layout.fillMaxSize`nimport androidx.compose.foundation.layout.height" `
  "import androidx.compose.foundation.layout.fillMaxSize`nimport androidx.compose.foundation.layout.fillMaxWidth`nimport androidx.compose.foundation.layout.height"

# ---- 10) BirthdayCard: sp import, Box contentAlignment, TextUnit ----
Patch 'ui\components\BirthdayCard.kt' `
  "import androidx.compose.ui.unit.dp`nimport com.remindly.app.core.domain.model.Person" `
  "import androidx.compose.ui.unit.dp`nimport androidx.compose.ui.unit.sp`nimport com.remindly.app.core.domain.model.Person"
Patch 'ui\components\BirthdayCard.kt' `
  'letterSpacing = androidx.compose.ui.unit.sp(2f),' `
  'letterSpacing = 2.sp,'
Patch 'ui\components\BirthdayCard.kt' `
  'letterSpacing = androidx.compose.ui.unit.sp(1.4f),' `
  'letterSpacing = 1.4.sp,'
# Box: horizontalAlignment -> contentAlignment (only the Box one, after padding vertical 14)
$rel = 'ui\components\BirthdayCard.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if ($t.Contains('contentAlignment = Alignment.Center')) { Write-Host "[ok]   $rel box" }
  else {
    $nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }
    $old = ".padding(vertical = 14.dp),${nl}                    horizontalAlignment = Alignment.CenterHorizontally,"
    $new = ".padding(vertical = 14.dp),${nl}                    contentAlignment = Alignment.Center,"
    if ($t.Contains($old)) {
      [IO.File]::WriteAllText($path, $t.Replace($old, $new))
      Write-Host "[FIX]  $rel box"
    } else {
      # try without exact indent
      $t2 = $t -replace '(\.padding\(vertical = 14\.dp\),\r?\n\s*)horizontalAlignment = Alignment\.CenterHorizontally', '$1contentAlignment = Alignment.Center'
      if ($t2 -ne $t) { [IO.File]::WriteAllText($path, $t2); Write-Host "[FIX]  $rel box (regex)" }
      else { Write-Host "[MISS] $rel box" }
    }
  }
}

# ---- 11) Pills: BorderStroke typing ----
$rel = 'ui\components\Pills.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  $changed = $false
  if (-not $t.Contains('val stroke: BorderStroke?')) {
    $t2 = $t -replace 'val border = if \(selected \|\| gradient\) Color\.Transparent\s*\r?\n\s*else BorderStroke\(1\.dp, MaterialTheme\.colorScheme\.outline\.copy\(alpha = 0\.7f\)\)',
      "val stroke: BorderSpan? = REPLACE_ME"
    # do simple line-based replace instead
    $lines = [IO.File]::ReadAllLines($path)
    $out = New-Object System.Collections.Generic.List[string]
    $i = 0
    while ($i -lt $lines.Count) {
      if ($lines[$i] -match 'val border = if \(selected \|\| gradient\) Color\.Transparent') {
        # skip this and next line (else BorderStroke...), insert typed stroke
        $out.Add('    val stroke: BorderStroke? = if (selected || gradient) {')
        $out.Add('        null')
        $out.Add('    } else {')
        $out.Add('        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))')
        $out.Add('    }')
        # skip until we consumed the else BorderStroke line
        while ($i -lt $lines.Count -and $lines[$i] -notmatch 'else BorderStroke\(1\.dp') { $i++ }
        $i++ # skip the else BorderStroke line itself
        $changed = $true
        continue
      }
      $out.Add($lines[$i]); $i++
    }
    if ($changed) {
      [IO.File]::WriteAllLines($path, $out)
      $t = [IO.File]::ReadAllText($path)
      Write-Host "[FIX]  $rel stroke decl"
    }
  }
  if ($t.Contains('it.border(border, shape)')) {
    $t = $t.Replace('.let { if (border != null) it.border(border, shape) else it }', '.then(if (stroke != null) Modifier.border(stroke, shape) else Modifier)')
    [IO.File]::WriteAllText($path, $t)
    Write-Host "[FIX]  $rel border call"
  } elseif ($t.Contains('Modifier.border(stroke, shape)')) {
    Write-Host "[ok]   $rel border call"
  } else { Write-Host "[MISS] $rel border call" }
}

# ---- 12) ReminderCard: OptIn (ASCII only patterns) ----
$rel = 'ui\components\ReminderCard.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if ($t.Contains('ExperimentalMaterial3Api')) { Write-Host "[ok]   $rel" }
  else {
    $nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }
    $needle = "@Composable${nl}fun ReminderCard("
    $repl   = "@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)${nl}@Composable${nl}fun ReminderCard("
    if ($t.Contains($needle)) {
      [IO.File]::WriteAllText($path, $t.Replace($needle, $repl))
      Write-Host "[FIX]  $rel"
    } else { Write-Host "[MISS] $rel" }
  }
}

# ---- 13) Scaffold: clickable ----
Patch 'ui\navigation\Scaffold.kt' `
  "import androidx.compose.foundation.background`nimport androidx.compose.foundation.layout.Arrangement" `
  "import androidx.compose.foundation.background`nimport androidx.compose.foundation.clickable`nimport androidx.compose.foundation.interaction.MutableInteractionSource`nimport androidx.compose.foundation.layout.Arrangement"
$rel = 'ui\navigation\Scaffold.kt'
$path = Join-Path $base $rel
if (Test-Path $path) {
  $t = [IO.File]::ReadAllText($path)
  if ($t.Contains('androidx.compose.foundation.clickable(')) {
    $t = $t.Replace(@'
private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
'@, @'
private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
'@)
    [IO.File]::WriteAllText($path, $t)
    if ($t.Contains('androidx.compose.foundation.clickable(')) { Write-Host "[MISS] $rel clickable body" }
    else { Write-Host "[FIX]  $rel clickable body" }
  } else { Write-Host "[ok]   $rel clickable body" }
}

# ---- 14) strings.xml duplicate next ----
$str = Join-Path $root 'app\src\main\res\values\strings.xml'
if (Test-Path $str) {
  $lines = [IO.File]::ReadAllLines($str)
  $seen = $false
  $out = New-Object System.Collections.Generic.List[string]
  foreach ($l in $lines) {
    if ($l -match 'name="next"') {
      if ($seen) { Write-Host '[FIX]  duplicate next removed'; continue }
      $seen = $true
    }
    $out.Add($l)
  }
  [IO.File]::WriteAllLines($str, $out)
  Write-Host '[ok]   strings.xml'
}

Write-Host ''
Write-Host 'DONE. Android Studio: File -> Sync Project with Gradle Files'
Write-Host 'Then: Build -> Rebuild Project'