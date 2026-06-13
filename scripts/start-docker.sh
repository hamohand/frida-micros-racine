export WINDOWS_HOST_IP=$(ip route show default | awk '{print $3}')
docker compose down
docker compose up -d
