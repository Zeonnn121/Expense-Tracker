# Run this from your project root (where parser-core folder lives)
# Usage: .\generate_registry.ps1

$bankFolder = "parser-core\src\main\kotlin\com\ritesh\parser\core\bank"
$outputFile = "app\src\main\java\com\example\expensetracker\BankParserRegistry.kt"

if (-not (Test-Path $bankFolder)) {
    Write-Host "ERROR: Could not find the bank folder at $bankFolder" -ForegroundColor Red
    Write-Host "Run this script from your project root, or edit the bankFolder path at the top of this script." -ForegroundColor Red
    exit 1
}

$files = Get-ChildItem -Path $bankFolder -Filter "*.kt" | Sort-Object Name

$classNames = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $pattern = 'class\s+(\w+)\s*(\([^)]*\))?\s*:\s*BankParser'
    if ($content -match $pattern) {
        $className = $matches[1]
        $classNames.Add($className)
    } else {
        Write-Host "WARNING: no class extending BankParser found in $($file.Name)" -ForegroundColor Yellow
    }
}

if ($classNames.Count -eq 0) {
    Write-Host "ERROR: No bank parser classes found." -ForegroundColor Red
    exit 1
}

$importLines = New-Object System.Collections.Generic.List[string]
$listLines = New-Object System.Collections.Generic.List[string]

foreach ($name in $classNames) {
    $importLines.Add("import com.ritesh.parser.core.bank.$name")
    $listLines.Add("        $name(),")
}

$imports = [string]::Join([Environment]::NewLine, $importLines)
$listEntries = [string]::Join([Environment]::NewLine, $listLines)

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("package com.example.expensetracker")
$lines.Add("")
$lines.Add("import com.ritesh.parser.core.bank.BankParser")
$lines.Add($imports)
$lines.Add("")
$lines.Add("object BankParserRegistry {")
$lines.Add("    private val parsers: List<BankParser> = listOf(")
$lines.Add($listEntries)
$lines.Add("    )")
$lines.Add("")
$lines.Add("    fun findParser(sender: String): BankParser? =")
$lines.Add("        parsers.firstOrNull { it.canHandle(sender) }")
$lines.Add("}")

$output = [string]::Join([Environment]::NewLine, $lines)

$outputDir = Split-Path $outputFile
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
[System.IO.File]::WriteAllText($outputFile, $output, [System.Text.Encoding]::UTF8)

Write-Host "Done. Found $($classNames.Count) bank parsers." -ForegroundColor Green
Write-Host "Written to $outputFile" -ForegroundColor Green
