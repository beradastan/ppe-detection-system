$files = Get-ChildItem -Path .\ -Include *.py -Recurse | Where-Object { $_.FullName -notmatch '\\venv\\' -and $_.FullName -notmatch '\\.git\\' }

foreach ($file in $files) {
    Write-Host "Processing: $($file.FullName)"
    $content = Get-Content $file.FullName -Raw
    
    # Remove single-line comments
    $content = $content -replace '(?m)^\s*#.*\n?', ''
    
    # Remove empty lines
    $lines = $content -split "`n" | Where-Object { $_.Trim() -ne '' }
    
    # Write back to file
    $lines | Set-Content $file.FullName -NoNewline:$false
}

Write-Host "All comments have been removed from Python files."
