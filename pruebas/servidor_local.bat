@echo off
chcp 65001 >nul
title Servidor Local - Finanzas Personales PWA
color 0B
echo =====================================================================
echo   Iniciando Servidor de Desarrollo PWA (Finanzas Personales)
echo   Acceso: http://localhost:3000
echo =====================================================================
cd ..
start "" http://localhost:3000
python -m http.server 3000
pause
