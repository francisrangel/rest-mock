# Rolls back a release that hasn't been published to Central yet.
#
# What it does:
#   1. Deletes the local git tag
#   2. Deletes the remote git tag (cancels CI if not yet started; prevents
#      re-deploys if it already finished)
#   3. Lists in-flight Central Portal deployments matching this version so
#      you can drop them. If MAVEN_CENTRAL_USERNAME/MAVEN_CENTRAL_PASSWORD
#      are in your environment, it can drop them for you with -Drop.
#
# What it does NOT do:
#   - Touch the "release: X.Y.Z" commit on master. If you want it gone:
#       git revert HEAD          # safe, leaves history visible
#       git reset --hard HEAD~1  # destructive, only if not yet pushed
#   - Roll back an already-published Central release. Central artifacts
#     are immutable. Publish a new version instead.

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Version,

    [switch]$Drop
)

$ErrorActionPreference = 'Stop'

if ($Version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$') {
    Write-Error "Version '$Version' is not a valid semver (e.g. 0.1.0 or 1.0.0-RC1)."
}

$Tag = "v$Version"

# --- 1. local tag ---------------------------------------------------------
git rev-parse -q --verify "refs/tags/$Tag" > $null 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "==> Deleting local tag $Tag"
    git tag -d $Tag
} else {
    Write-Host "==> Local tag $Tag not present, skipping"
}

# --- 2. remote tag --------------------------------------------------------
$remoteTags = git ls-remote --tags origin $Tag 2>$null
if ($remoteTags -match "refs/tags/$([regex]::Escape($Tag))") {
    Write-Host "==> Deleting remote tag $Tag"
    git push --delete origin $Tag
} else {
    Write-Host "==> Remote tag $Tag not present, skipping"
}

# --- 3. Central Portal staging --------------------------------------------
Write-Host "==> Checking Central Portal for staged deployments of $Version"

$centralUser = $env:MAVEN_CENTRAL_USERNAME
$centralPass = $env:MAVEN_CENTRAL_PASSWORD

if (-not $centralUser -or -not $centralPass) {
    Write-Host @"

MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD not in environment, so I
can't query the Central Portal API. Open this URL and drop any deployment
for 'org.simulatest' at version ${Version}:

    https://central.sonatype.com/publishing/deployments

Droppable statuses: VALIDATED, FAILED.
PENDING and VALIDATING must finish processing before they can be dropped.
PUBLISHING and PUBLISHED are past the point of no return.

"@
    exit 0
}

$pair  = "${centralUser}:${centralPass}"
$token = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$api   = "https://central.sonatype.com/api/v1/publisher"
$headers = @{ Authorization = "Bearer $token" }

try {
    $response = Invoke-RestMethod -Method Post `
        -Uri "$api/deployments?pageNum=0&pageSize=50" `
        -Headers $headers
} catch {
    Write-Error "Could not reach Central Portal API; check credentials/network. $_"
}

# The Central Portal API wraps results under `.deployments` but older
# shapes returned the array directly. Accept either.
$deployments = if ($response.deployments) { $response.deployments }
               elseif ($response -is [System.Collections.IEnumerable]) { $response }
               else { @() }

if (-not $deployments) {
    Write-Host "No Central staging deployment matched version $Version."
    exit 0
}

# Match by the version appearing as a token in the deployment name
# (so 1.2.3 doesn't also match 1x2x3).
$versionRe = [regex]::Escape($Version)
$matched = $deployments | Where-Object {
    $_.deploymentName -match "[:-]${versionRe}([`",\-]|$)"
}

if (-not $matched) {
    Write-Host "No Central staging deployment matched version $Version."
    exit 0
}

Write-Host ""
Write-Host "Matching staged deployments:"
$matched | ForEach-Object {
    Write-Host ("  {0}  [{1}]  {2}" -f $_.deploymentId, $_.deploymentState, $_.deploymentName)
}
Write-Host ""

if (-not $Drop) {
    Write-Host "Re-run with -Drop to delete them, or do it in the Central Portal UI:"
    Write-Host "    https://central.sonatype.com/publishing/deployments"
    exit 0
}

# Only VALIDATED and FAILED are droppable per the Central Portal API.
$droppable = $matched | Where-Object { $_.deploymentState -in @('VALIDATED', 'FAILED') }
if (-not $droppable) {
    Write-Error @"
No matched deployments are currently in a droppable state
(VALIDATED or FAILED). PENDING/VALIDATING must finish first.
"@
}

foreach ($d in $droppable) {
    Write-Host "==> Dropping deployment $($d.deploymentId)"
    try {
        Invoke-RestMethod -Method Delete `
            -Uri "$api/deployment/$($d.deploymentId)" `
            -Headers $headers | Out-Null
        Write-Host "    dropped"
    } catch {
        Write-Error "    failed to drop $($d.deploymentId): $_"
    }
}

Write-Host ""
Write-Host "Done. The 'release: $Version' commit on master is untouched -"
Write-Host "revert it manually if you don't want it in the next release."
