# ============================================================================
# Script para gerar JWT_SECRET no Windows
# ============================================================================
# Uso: .\generate-jwt-secret.ps1
# ============================================================================

Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "  Gerador de JWT_SECRET para LALUR V2 ECF API" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Gerar chave aleatória de 64 bytes em Base64
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
$rng.Dispose()

Write-Host "`nSua chave JWT_SECRET foi gerada com sucesso!" -ForegroundColor Green
Write-Host "`n--------------------------------------------------" -ForegroundColor Yellow
Write-Host "JWT_SECRET:" -ForegroundColor White
Write-Host $secret -ForegroundColor Green
Write-Host "--------------------------------------------------" -ForegroundColor Yellow

Write-Host "`nComo usar no Render:" -ForegroundColor Cyan
Write-Host "1. Acesse seu Web Service no Render" -ForegroundColor White
Write-Host "2. Vá em Environment > Add Environment Variable" -ForegroundColor White
Write-Host "3. Key: JWT_SECRET" -ForegroundColor White
Write-Host "4. Value: Cole a chave acima" -ForegroundColor White

Write-Host "`nComo usar localmente (.env):" -ForegroundColor Cyan
Write-Host "JWT_SECRET=$secret" -ForegroundColor White

Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "IMPORTANTE: Guarde esta chave em local seguro!" -ForegroundColor Red
Write-Host "Não compartilhe e não faça commit no Git!" -ForegroundColor Red
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Copiar para clipboard (opcional)
try {
    Set-Clipboard -Value $secret
    Write-Host "[✓] Chave copiada para a área de transferência!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "[!] Não foi possível copiar automaticamente. Copie manualmente." -ForegroundColor Yellow
    Write-Host ""
}
