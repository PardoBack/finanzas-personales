@echo off
chcp 65001 >nul
title Finanzas Personales - Centro de Pruebas
color 0A

:MENU
cls
echo =====================================================================
echo           FINANZAS PERSONALES PRO - CENTRO DE PRUEBAS
echo =====================================================================
echo.
echo   [1] Iniciar Servidor Web Local y Abrir PWA (Dashboard + App Movil)
echo   [2] Ejecutar Suite de Pruebas Unitarias (Archivos, Excel, Formulas)
echo   [3] Probar Motor NLP Interactivo (Slang colombiano: lucas, mil, etc.)
echo   [4] Probar Analizador Financiero 50/30/20
echo   [5] Generar Reporte de Exportacion Excel
echo   [6] Ver Estado de Git / Cambios pendientes
echo   [7] Sincronizar y Subir Cambios a GitHub (git push)
echo   [8] Salir
echo.
echo =====================================================================
set /p OPCION="Selecciona una opcion (1-8): "

if "%OPCION%"=="1" goto SERVIDOR
if "%OPCION%"=="2" goto TESTS
if "%OPCION%"=="3" goto NLP
if "%OPCION%"=="4" goto ANALIZADOR
if "%OPCION%"=="5" goto EXCEL
if "%OPCION%"=="6" goto GIT
if "%OPCION%"=="7" goto PUSH
if "%OPCION%"=="8" goto FIN

echo Opcion no valida. Presiona cualquier tecla para continuar...
pause >nul
goto MENU

:SERVIDOR
cls
echo =====================================================================
echo   Iniciando Servidor Local en el puerto 3000...
echo =====================================================================
cd ..
start "" http://localhost:3000
python -m http.server 3000
cd pruebas
pause
goto MENU

:TESTS
cls
echo =====================================================================
echo   Ejecutando Suite de Pruebas Automatizadas...
echo =====================================================================
python test_suite.py
echo.
pause
goto MENU

:NLP
cls
echo =====================================================================
echo   Probador de NLP con Slang Colombiano...
echo =====================================================================
python probar_nlp.py
pause
goto MENU

:ANALIZADOR
cls
echo =====================================================================
echo   Ejecutando Analizador Financiero 50/30/20...
echo =====================================================================
cd ..
python python_tools\financial_analyzer.py
cd pruebas
pause
goto MENU

:EXCEL
cls
echo =====================================================================
echo   Generando Reporte Excel Formateado...
echo =====================================================================
cd ..
python python_tools\export_to_excel.py
cd pruebas
pause
goto MENU

:GIT
cls
echo =====================================================================
echo   Estado del Repositorio Git
echo =====================================================================
cd ..
git status
echo.
git log -n 5 --oneline
cd pruebas
pause
goto MENU

:PUSH
cls
echo =====================================================================
echo   Sincronizando y Subiendo Cambios a GitHub...
echo =====================================================================
cd ..
git status -s
set /p MSG="Mensaje del commit (o Enter para 'actualizacion automatica'): "
if "%MSG%"=="" set MSG=actualizacion automatica
git add .
git commit -m "%MSG%"
git push origin main
echo.
echo Operacion completada.
cd pruebas
pause
goto MENU

:FIN
exit
