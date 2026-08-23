# One-time setup. Pushes the four secrets the release workflow needs into
# this GitHub repo via `gh`. Re-running is safe: gh overwrites existing
# values.
#
# Prerequisites:
#   * gh CLI installed and authenticated (`gh auth status`)
#   * Local GPG key generated (`gpg --list-secret-keys`)
#   * Central Portal user token created at:
#       https://central.sonatype.com/account
#
# After this finishes you should also publish the public key to a
# keyserver so Central can verify signatures:
#   gpg --send-keys <KEY_ID> --keyserver keys.openpgp.org

$ErrorActionPreference = 'Stop'

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "gh CLI not found. Install from https://cli.github.com/"
}

gh auth status > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Not logged in to gh. Run: gh auth login"
}

Write-Host "Available GPG secret keys:"
gpg --list-secret-keys --keyid-format=long
Write-Host ""
$gpgKeyId = Read-Host "GPG key ID to use (long form, e.g. 2C51144F74D9EC68)"

# Empty input would make `gpg --export-secret-keys` export EVERY key in
# the local keyring, which then ships to GitHub as one repo secret.
# Validate the ID is non-empty AND actually present.
if ([string]::IsNullOrWhiteSpace($gpgKeyId)) {
    Write-Error "GPG key ID is required."
}
gpg --list-secret-keys $gpgKeyId > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "No secret key found for ID: $gpgKeyId"
}

Write-Host "==> Exporting secret key and uploading as MAVEN_GPG_PRIVATE_KEY"
$exportedKey = gpg --armor --export-secret-keys $gpgKeyId | Out-String
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($exportedKey)) {
    Write-Error "gpg export failed."
}
$exportedKey | gh secret set MAVEN_GPG_PRIVATE_KEY
if ($LASTEXITCODE -ne 0) { Write-Error "gh secret set MAVEN_GPG_PRIVATE_KEY failed." }

$gpgPass = Read-Host "GPG passphrase" -AsSecureString
$gpgPassPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($gpgPass)
)
Write-Host "==> Uploading MAVEN_GPG_PASSPHRASE"
$gpgPassPlain | gh secret set MAVEN_GPG_PASSPHRASE
if ($LASTEXITCODE -ne 0) { Write-Error "gh secret set MAVEN_GPG_PASSPHRASE failed." }
Remove-Variable gpgPassPlain

$centralUser = Read-Host "Central Portal user-token USERNAME"
Write-Host "==> Uploading MAVEN_CENTRAL_USERNAME"
$centralUser | gh secret set MAVEN_CENTRAL_USERNAME
if ($LASTEXITCODE -ne 0) { Write-Error "gh secret set MAVEN_CENTRAL_USERNAME failed." }

$centralPass = Read-Host "Central Portal user-token PASSWORD" -AsSecureString
$centralPassPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($centralPass)
)
Write-Host "==> Uploading MAVEN_CENTRAL_PASSWORD"
$centralPassPlain | gh secret set MAVEN_CENTRAL_PASSWORD
if ($LASTEXITCODE -ne 0) { Write-Error "gh secret set MAVEN_CENTRAL_PASSWORD failed." }
Remove-Variable centralPassPlain

Write-Host ""
Write-Host "Done. Verify with: gh secret list"
Write-Host ""
Write-Host "Don't forget to publish your public key:"
Write-Host "    gpg --send-keys $gpgKeyId --keyserver keys.openpgp.org"
