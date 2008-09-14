#/bin/bash

CC=i686-mingw32-gcc
JAVA_HOME=~/jdk1.5.0_15.win32
CFLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/win32 -O3"
LFLAGS="-shared -Wl,--kill-at -L$JAVA_HOME/lib -ljawt"

$CC $CFLAGS MinSize.c -o minsize.dll $LFLAGS
