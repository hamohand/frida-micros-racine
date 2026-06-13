@echo off
echo =========================================
echo   Passage en mode DEVELOPPEMENT Frontend
echo =========================================
echo.
echo 1) Arret du frontend dans Docker pour liberer le port 4200...
docker compose stop frontend
echo.
echo 2) Lancement du serveur de developpement Angular (Hot Reload)...
cd frontend
call npm start
pause
