# Tutoriel de Déploiement : Simulateur Autonome sur VPS

Ce guide détaille les étapes exactes pour déployer uniquement le simulateur sur votre VPS Ubuntu/Debian et le rendre accessible via `https://simul-frida.enclume-numerique.com`.

## 1. Préparation du VPS

Connectez-vous à votre VPS en SSH :
```bash
ssh root@votre_ip_vps
```

Mettez à jour le système et installez Docker, Docker Compose, Git et Nginx :
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose git nginx certbot python3-certbot-nginx
```

Assurez-vous que Docker démarre automatiquement :
```bash
sudo systemctl enable docker
sudo systemctl start docker
```

## 2. Récupération du code

Clonons le code source du simulateur :
```bash
mkdir -p /opt/frida
cd /opt/frida
git clone https://github.com/hamohand/frida-micros-racine.git
cd frida-micros-racine
```

## 3. Lancement des conteneurs (Backend + Frontend + DB)

Nous allons utiliser le fichier de configuration allégé `docker-compose.vps-simulateur.yml`. Il fera tourner les services uniquement en local sur le VPS (`127.0.0.1`), c'est Nginx qui fera le pont vers l'extérieur pour plus de sécurité.

```bash
sudo docker-compose -f docker-compose.vps-simulateur.yml up -d --build
```

Attendez quelques minutes que les images se construisent et que les conteneurs démarrent.
Pour vérifier que tout tourne bien :
```bash
sudo docker ps
```
*Vous devriez voir `db`, `backend` (port 8080) et `frontend` (port 4200).*

## 4. Configuration Nginx (Reverse Proxy)

Créez le fichier de configuration Nginx pour votre sous-domaine :
```bash
sudo nano /etc/nginx/sites-available/simul-frida
```

Collez cette configuration :
```nginx
server {
    listen 80;
    server_name simul-frida.enclume-numerique.com;

    # Frontend (Angular)
    location / {
        proxy_pass http://127.0.0.1:4200;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Activez le site et relancez Nginx :
```bash
sudo ln -s /etc/nginx/sites-available/simul-frida /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 5. Sécurisation HTTPS (Certbot)

Assurez-vous que votre nom de domaine `simul-frida.enclume-numerique.com` pointe bien vers l'IP de votre VPS (Enregistrement A chez votre hébergeur/bureau d'enregistrement).

Lancez la création du certificat SSL :
```bash
sudo certbot --nginx -d simul-frida.enclume-numerique.com
```
Laissez-vous guider (entrez une adresse email, acceptez les conditions, et choisissez l'option de redirection automatique HTTP -> HTTPS).

## C'est terminé ! 🎉

Le système est désormais en ligne. L'application détectera automatiquement que l'URL est `simul-frida.enclume-numerique.com` et **masquera complètement les menus administrateurs**, redirigeant tous les visiteurs vers le simulateur pur.
