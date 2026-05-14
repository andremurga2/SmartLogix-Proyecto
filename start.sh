#!/bin/bash
echo "╔══════════════════════════════════════════════════╗"
echo "║        SmartLogix — Iniciando servicios          ║"
echo "║  RabbitMQ · ms-auth · ms-inventario · ms-pedidos ║"
echo "║              BFF · Frontend                      ║"
echo "╚══════════════════════════════════════════════════╝"

# Cargar variables de entorno si existe .env
if [ -f .env ]; then
    echo "✅ Cargando variables desde .env"
    export $(grep -v '^#' .env | xargs)
fi

echo ""
echo "🔨 Construyendo imágenes Docker..."
docker compose build

echo ""
echo "🚀 Levantando todos los servicios..."
docker compose up -d

echo ""
echo "⏳ Esperando que los servicios estén listos..."
sleep 10

docker compose ps

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║              ¡Sistema listo!                     ║"
echo "╠══════════════════════════════════════════════════╣"
echo "║  🌐 Frontend  → http://localhost:3000            ║"
echo "║  🔑 ms-auth   → http://localhost:8083            ║"
echo "║  📦 BFF/GW    → http://localhost:9090            ║"
echo "║  🐰 RabbitMQ  → http://localhost:15672           ║"
echo "║                 (usuario: guest / guest)         ║"
echo "╠══════════════════════════════════════════════════╣"
echo "║  👤 admin / admin123  (rol ADMIN)                ║"
echo "║  👤 user1 / password123 (rol USER)               ║"
echo "╚══════════════════════════════════════════════════╝"
