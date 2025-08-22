#!/bin/bash

echo "PPE Detection Frontend - React Uygulaması Başlatılıyor..."

# Node.js ve npm kontrolü
if ! command -v node &> /dev/null; then
    echo "Node.js yüklü değil. Lütfen Node.js'i yükleyin."
    echo "https://nodejs.org/en/download/"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "npm yüklü değil. Lütfen npm'i yükleyin."
    exit 1
fi

# node_modules kontrolü ve kurulum
if [ ! -d "node_modules" ]; then
    echo "Bağımlılıklar yükleniyor..."
    npm install
fi

# Backend kontrolü
echo "Backend kontrolü yapılıyor..."
if ! curl -s http://localhost:8000/health > /dev/null; then
    echo " Backend çalışmıyor. Lütfen önce backend'i başlatın:"
    echo "   cd ../backend && ./start.sh"
    echo ""
    echo "Backend başlatma bekleniyor... (5 saniye)"
    sleep 5
    
    if ! curl -s http://localhost:8000/health > /dev/null; then
        echo "Backend hala çalışmıyor. Manuel olarak başlatın."
        echo "   cd ../backend && ./start.sh"
        exit 1
    fi
fi

echo "Backend çalışıyor"

# React uygulamasını başlat
echo "React uygulaması başlatılıyor..."
echo "Uygulama: http://localhost:3000"
echo "Backend API: http://localhost:8000"
echo ""
echo "Durdurmak için Ctrl+C tuşlayın..."

npm start
