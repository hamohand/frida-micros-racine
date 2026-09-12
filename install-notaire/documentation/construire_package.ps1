<#
.SYNOPSIS
    Construit le package d'installation notaire (Composante 1) a envoyer a un client.

.DESCRIPTION
    1) Regenere frida-micros.zip depuis les sources du depot (backend, frontend, ocr-api,
       docker-compose.local.yml, .env.local) : voir INSTALLATION_PROCEDURE.md.
    2) Assemble les 7 fichiers livrables de install-notaire/docker-installation/ dans
       FRIDA-Installation.zip.
    3) Controle le resultat (fichiers presents, rien de compromettant dans .env.local,
       aucun fichier parasite).

    A lancer depuis n'importe quel dossier : le chemin du depot est deduit de
    l'emplacement de ce script (install-notaire/documentation/).

.PARAMETER Destination
    Dossier ou ecrire FRIDA-Installation.zip. Par defaut, le Bureau de l'utilisateur courant.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File install-notaire\documentation\construire_package.ps1

.EXAMPLE
    .\construire_package.ps1 -Destination C:\Livraisons
#>
param(
    [string]$Destination = [Environment]::GetFolderPath("Desktop")
)

$ErrorActionPreference = "Stop"

# install-notaire/documentation/construire_package.ps1 -> racine du depot = deux niveaux au-dessus
$depot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$source = Join-Path $depot "install-notaire\docker-installation"
$fichiersLivres = @("Installer-Frida.bat", "Installer-Frida.ps1", "LISEZMOI.txt", "desinstaller.bat",
                     "frida-micros.zip", "restaurer.bat", "sauvegarder.bat")

function Write-Etape($texte) {
    Write-Host ""
    Write-Host "=== $texte ===" -ForegroundColor Cyan
}

# -----------------------------------------------------------------------------
# 1) Regenerer frida-micros.zip depuis les sources actuelles du depot
# -----------------------------------------------------------------------------
Write-Etape "1/3 - Regeneration de frida-micros.zip"

$stage = Join-Path $env:TEMP ("frida-zip-stage-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $stage -Force | Out-Null

try {
    $flags = @("/E", "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS", "/NP")
    & robocopy (Join-Path $depot "backend")  (Join-Path $stage "backend")  @flags /XD target uploads | Out-Null
    & robocopy (Join-Path $depot "frontend") (Join-Path $stage "frontend") @flags /XD node_modules dist .angular | Out-Null
    # /XF *.log : des outils locaux (synchronisation, runs de test) laissent des journaux dans
    # ocr-api/app_ocr ; aucun n'appartient au livrable (voir commit 88ba459).
    & robocopy (Join-Path $depot "ocr-api")  (Join-Path $stage "ocr-api")  @flags /XF *.log | Out-Null

    Copy-Item (Join-Path $source "docker-compose.local.yml") $stage
    Copy-Item (Join-Path $source ".env.local") $stage

    $zipSources = Join-Path $source "frida-micros.zip"
    if (Test-Path $zipSources) { Remove-Item $zipSources -Force }
    Compress-Archive -Path "$stage\*" -DestinationPath $zipSources -CompressionLevel Optimal

    $nbFichiers = (Get-ChildItem $stage -Recurse -File).Count
    Write-Host ("frida-micros.zip regenere : {0} fichiers, {1} Ko" -f $nbFichiers, [math]::Round((Get-Item $zipSources).Length / 1KB)) -ForegroundColor Green
} finally {
    Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
}

# -----------------------------------------------------------------------------
# 2) Controles avant envoi (voir INSTALLATION_PROCEDURE.md : "Ce qu'il faut avant d'envoyer")
# -----------------------------------------------------------------------------
Write-Etape "2/3 - Controles avant envoi"

$envContent = Get-Content (Join-Path $source ".env.local") -Raw
foreach ($cle in @("ADMIN_PASSWORD", "JWT_SECRET")) {
    if ($envContent -match "(?m)^[ `t]*$cle[ `t]*=[ `t]*\S") {
        Write-Host ("ATTENTION : $cle a une valeur dans .env.local. Il doit rester vide : " +
                     "chaque poste doit recevoir la sienne a l'installation.") -ForegroundColor Yellow
    } else {
        Write-Host "$cle vide dans .env.local : ok" -ForegroundColor Green
    }
}

foreach ($f in $fichiersLivres) {
    if (-not (Test-Path (Join-Path $source $f))) {
        throw "Fichier manquant dans $source : $f"
    }
}

# -----------------------------------------------------------------------------
# 3) Assemblage de FRIDA-Installation.zip
# -----------------------------------------------------------------------------
Write-Etape "3/3 - Assemblage de FRIDA-Installation.zip"

$etape = Join-Path $env:TEMP ("frida-package-stage-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $etape -Force | Out-Null

if (-not (Test-Path $Destination)) { New-Item -ItemType Directory -Path $Destination -Force | Out-Null }
$cible = Join-Path $Destination "FRIDA-Installation.zip"

try {
    foreach ($f in $fichiersLivres) { Copy-Item (Join-Path $source $f) $etape }

    if (Test-Path $cible) { Remove-Item $cible -Force }
    Compress-Archive -Path "$etape\*" -DestinationPath $cible -CompressionLevel Optimal

    # Controle : les 7 fichiers sont bien dans le zip livre, et frida-micros.zip transporte
    # docker-compose.local.yml + .env.local a sa racine (sinon l'installeur ne les trouve pas).
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $livre = [System.IO.Compression.ZipFile]::OpenRead($cible)
    $noms = $livre.Entries | ForEach-Object { $_.FullName }
    $manquants = $fichiersLivres | Where-Object { $noms -notcontains $_ }
    if ($manquants) { $livre.Dispose(); throw "Fichier(s) absent(s) du zip livre : $($manquants -join ', ')" }

    $interne = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $etape "frida-micros.zip"))
    $nomsInternes = $interne.Entries | ForEach-Object { $_.FullName }
    foreach ($attendu in @(".env.local", "docker-compose.local.yml")) {
        if ($nomsInternes -notcontains $attendu) {
            $interne.Dispose(); $livre.Dispose()
            throw "$attendu absent de la racine de frida-micros.zip"
        }
    }
    $parasites = $nomsInternes | Where-Object { $_ -match '\.log$' -or $_ -match '\[conflicted' }
    if ($parasites) {
        Write-Host ("ATTENTION : fichier(s) parasite(s) dans frida-micros.zip : " + ($parasites -join ", ")) -ForegroundColor Yellow
    }
    $interne.Dispose()
    $livre.Dispose()

    Write-Host ("Package livrable : " + ($noms -join ", ")) -ForegroundColor Green
    Write-Host ("{0} : {1} Ko, {2}" -f $cible, [math]::Round((Get-Item $cible).Length / 1KB), (Get-Item $cible).LastWriteTime) -ForegroundColor Green
} finally {
    Remove-Item $etape -Recurse -Force -ErrorAction SilentlyContinue
}
