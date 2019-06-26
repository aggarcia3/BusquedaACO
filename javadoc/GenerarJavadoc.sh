#!/bin/sh
echo '**********************'
echo '* GENERACIÓN JAVADOC *'
echo '**********************'
echo

if [ "$1" != '-s' -a "$1" != '--jason-source' ]; then
    printf "Introduce el directorio del código fuente de Jason ($HOME/jason-2.3/src/main/java): "
    read -r dirCodigoFuente
    if [ -z "$dirCodigoFuente" ]; then
        dirCodigoFuente="$HOME/jason-2.3/src/main/java"
    fi
else
    dirCodigoFuente="$2"
fi
echo "Se buscará el código fuente de Jason en el directorio: $dirCodigoFuente"
echo

javadoc -sourcepath \""$dirCodigoFuente"\"\;.. @ParametrosJavadoc.txt

echo
echo '> Generación de Javadoc completada.'
if [ "$1" != '-s' -a "$1" != '--jason-source' ]; then
    printf 'Presiona una tecla para salir.'
    paramsTerminal=$(stty -g)
    stty -icanon -echo min 0 time 100
    dd bs=1 count=1 >/dev/null 2>&1
    stty "$paramsTerminal"
    echo
fi
