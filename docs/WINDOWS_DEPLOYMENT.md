# 🪟 Windows Server Zero-Cost 24/7 Deployment Guide

This guide describes how to configure and deploy **SyncFlow** on a Windows machine or Windows Server without spending any money on licenses or third-party server tools.

---

## 1. Architecture on Windows Server

```mermaid
graph LR
    Internet([Internet / Remote Devices]) -->|HTTPS / WSS| CF[Cloudflare Tunnel daemon: cloudflared.exe]
    CF -->|localhost:80| NGINX[Nginx for Windows]
    NGINX -->|/api and /ws| SB[Spring Boot 3.x - WinSW Service :8080]
    NGINX -->|/ (Static Assets)| ANG[Angular Web App Dist Folder]
    SB --> PG[(PostgreSQL Windows Service :5432)]
```

---

## 2. Spring Boot 3.x as a Windows Service (WinSW)

WinSW (Windows Service Wrapper) wraps your `.jar` file so it runs as a native background service that automatically boots with Windows.

### Step 1: Download WinSW
- Download the latest executable from GitHub: [WinSW Releases](https://github.com/winsw/winsw/releases) (e.g., `WinSW-x64.exe`).
- Place it in `C:\SyncFlow\backend\` and rename it to `syncflow-service.exe`.

### Step 2: Configuration `syncflow-service.xml`
Create `syncflow-service.xml` alongside `syncflow-service.exe`:

```xml
<service>
  <id>SyncFlowBackend</id>
  <name>SyncFlow Backend API</name>
  <description>Spring Boot 3.x with Java 21 Virtual Threads for SyncFlow Enterprise</description>
  <executable>java</executable>
  <arguments>-Xmx2048m -Dspring.profiles.active=prod -jar C:\SyncFlow\backend\syncflow-api.jar</arguments>
  <logmode>roll</logmode>
  <logpath>C:\SyncFlow\backend\logs</logpath>
  <startmode>Automatic</startmode>
  <delayedAutoStart>true</delayedAutoStart>
</service>
```

### Step 3: Install & Start Service
Open PowerShell as Administrator:
```powershell
cd C:\SyncFlow\backend
.\syncflow-service.exe install
.\syncflow-service.exe start
```

---

## 3. Nginx for Windows Configuration

Place Nginx in `C:\nginx`. Update `conf/nginx.conf`:

```nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Upstream to Spring Boot Virtual Threads
    upstream spring_boot_backend {
        server 127.0.0.1:8080;
        keepalive 32;
    }

    server {
        listen       80;
        server_name  localhost;

        # Angular Single Page App (SPA)
        location / {
            root   C:/SyncFlow/frontend/dist/syncflow/browser;
            index  index.html;
            try_files $uri $uri/ /index.html;
        }

        # REST API Proxy
        location /api/ {
            proxy_pass http://spring_boot_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # WebSocket STOMP Proxy
        location /ws-syncflow/ {
            proxy_pass http://spring_boot_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_read_timeout 86400s;
            proxy_send_timeout 86400s;
        }
    }
}
```

Run Nginx as a Windows Service using another WinSW wrapper (`nginx-service.xml`).

---

## 4. Free Public SSL & Remote Access: Cloudflare Tunnel

**Why Cloudflare Tunnel?**
- 100% Free.
- No public static IP needed.
- No router port-forwarding needed (safer, firewall friendly).
- Free automatic SSL certificates managed by Cloudflare.

### Setup Instructions:
1. Download `cloudflared.exe` from Cloudflare and put it in `C:\Cloudflared`.
2. Login to your free Cloudflare account:
   ```cmd
   cloudflared.exe tunnel login
   ```
3. Create the tunnel:
   ```cmd
   cloudflared.exe tunnel create syncflow-tunnel
   ```
4. Map routing in `config.yml`:
   ```yaml
   tunnel: <TUNNEL_UUID>
   credentials-file: C:\Users\<Username>\.cloudflared\<TUNNEL_UUID>.json

   ingress:
     - hostname: syncflow.yourdomain.com
       service: http://localhost:80
     - service: http_status:404
   ```
5. Install cloudflared as Windows Service:
   ```cmd
   cloudflared.exe service install
   ```

Now SyncFlow is accessible securely from any Android phone or web browser across the globe via `https://syncflow.yourdomain.com` with zero recurring hosting fees!
