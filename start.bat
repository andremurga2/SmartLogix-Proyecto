@echo off
echo ╔══════════════════════════════════════════════════╗
echo ║        SmartLogix — Iniciando servicios          ║
echo ║  RabbitMQ · ms-auth · ms-inventario · ms-pedidos ║
echo ║              BFF · Frontend                      ║
echo ╚══════════════════════════════════════════════════╝

if exist .env (
    echo [OK] Cargando variables desde .env
    for /f "tokens=*" %%i in (.env) do set %%i
)

echo.
echo [*] Construyendo imagenes Docker...
docker compose build

echo.
echo [*] Levantando todos los servicios...
docker compose up -d

echo.
echo [*] Esperando que los servicios esten listos...
timeout /t 10 /nobreak > nul

docker compose ps

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║              ¡Sistema listo!                     ║
echo ╠══════════════════════════════════════════════════╣
echo ║  Frontend  →  http://localhost:3000              ║
echo ║  ms-auth   →  http://localhost:8083              ║
echo ║  BFF/GW    →  http://localhost:9090              ║
echo ║  RabbitMQ  →  http://localhost:15672             ║
echo ║                (usuario: guest / guest)          ║
echo ╠══════════════════════════════════════════════════╣
echo ║  admin / admin123       (rol ADMIN)              ║
echo ║  user1 / password123    (rol USER)               ║
echo ╚══════════════════════════════════════════════════╝
pause
