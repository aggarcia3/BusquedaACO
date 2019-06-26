@rem Script de generación de Javadoc para Windows
@echo off
chcp 65001 > nul 2>&1
title Generar Javadoc

echo **********************
echo * GENERACIÓN JAVADOC *
echo **********************
echo.
if CMDEXTVERSION 1 if "%1" NEQ "-s" if "%1" NEQ "--jason-source" goto pedir_dir
set dirCodigoFuente=%2
goto generar_javadoc

:pedir_dir
set /p dirCodigoFuente="Introduce el directorio del código fuente de Jason (%USERPROFILE%\jason-2.3\src\main\java): "
if "%dirCodigoFuente%"=="" (set dirCodigoFuente=%USERPROFILE%\jason-2.3\src\main\java)

:generar_javadoc
echo Se buscará el código fuente de Jason en el directorio: %dirCodigoFuente%
echo.

javadoc -sourcepath "%dirCodigoFuente%";.. ^@ParametrosJavadoc^.txt

echo.
echo ^> Generación de Javadoc completada.
if CMDEXTVERSION 1 if "%1" NEQ "-s" if "%1" NEQ "--jason-source" goto tecla_salir
goto salir

:tecla_salir
echo Presiona una tecla o cierra esta ventana para salir.
pause > nul 2>&1

:salir