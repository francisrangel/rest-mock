# Cuts a Maven Central release.
#
# Local steps (this script):
#   1. Bumps the version in pom.xml
#   2. Bumps the reproducible build timestamp
#   3. Verifies the build is green
#   4. Commits, tags
#
# After this finishes, run:
#   git push origin master vX.Y.Z
#
# That tag push triggers .github/workflows/release.yml which signs the
# artifacts and uploads them to a Central Portal staging deployment.
# Final "Publish" click happens at https://central.sonatype.com/publishing
# (autoPublish is intentionally off for the first releases).

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$NewVersion
)

$ErrorActionPreference = 'Stop'

Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..'))

if ($NewVersion -notmatch '^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$') {
    Write-Error "Version '$NewVersion' is not a valid semver (e.g. 0.1.0 or 1.0.0-RC1)."
}

$Tag = "v$NewVersion"
$Today = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddT00:00:00Z")

git diff-index --quiet HEAD --
if ($LASTEXITCODE -ne 0) {
    Write-Error "Working tree has uncommitted changes. Commit or stash first."
}

git rev-parse -q --verify "refs/tags/$Tag" > $null 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Error "Tag $Tag already exists."
}

$CurrentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
if ($CurrentBranch -ne 'master') {
    Write-Error "Releases cut from 'master' only (currently on '$CurrentBranch')."
}

Write-Host "==> Bumping version to $NewVersion"
mvn -B -q versions:set `
    "-DnewVersion=$NewVersion" `
    "-DgenerateBackupPoms=false"
if ($LASTEXITCODE -ne 0) { Write-Error "mvn versions:set failed." }

Write-Host "==> Bumping reproducible build timestamp to $Today"
$pom = Get-Content pom.xml -Raw
$pom = [regex]::Replace(
    $pom,
    '<project\.build\.outputTimestamp>.*?</project\.build\.outputTimestamp>',
    "<project.build.outputTimestamp>$Today</project.build.outputTimestamp>"
)
Set-Content -Path pom.xml -Value $pom -NoNewline

Write-Host "==> Verifying build"
mvn -B verify
if ($LASTEXITCODE -ne 0) { Write-Error "mvn verify failed." }

Write-Host "==> Committing version bump"
git add -u
git commit -m "release: $NewVersion"
if ($LASTEXITCODE -ne 0) { Write-Error "git commit failed." }

Write-Host "==> Tagging $Tag"
git tag -a $Tag -m "Release $NewVersion"
if ($LASTEXITCODE -ne 0) { Write-Error "git tag failed." }

Write-Host @"

Local prep complete.

Next:
    git push origin master $Tag

That triggers .github/workflows/release.yml which signs and stages the
release at https://central.sonatype.com/publishing - click 'Publish' there
to release publicly.
"@
