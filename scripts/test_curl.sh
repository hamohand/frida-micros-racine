export WINDOWS_HOST_IP=$(ip route show default | awk '{print $3}')
curl -s -X POST -H 'Content-Type: application/json' -d '{"filename": "ocr_89339397-f9c2-422a-9af9-bec74b98cd13_1778032541449_cni_dz_recto.jpeg", "mode": "rapide"}' http://$WINDOWS_HOST_IP:8082/api/analyser
