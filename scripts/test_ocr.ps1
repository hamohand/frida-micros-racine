$entity = Invoke-RestMethod -Uri 'http://localhost:8082/api/entite/cni_algo_recto_01'
$zones = @{}
foreach ($z in $entity.zones) {
    $zones[$z.nom] = @{coords=$z.coords}
}
$payload = @{
    filename='ocr_89339397-f9c2-422a-9af9-bec74b98cd13_1778032541449_cni_dz_recto.jpeg'
    cadre_reference=$entity.cadre_reference
    zones=$zones
    mode='rapide'
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri 'http://localhost:8082/api/analyser' -Method Post -ContentType 'application/json' -Body $payload
