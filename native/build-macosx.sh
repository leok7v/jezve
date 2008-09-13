#/bin/bash

gcc -arch i386 -arch ppc MinSize.m -o libminsize.jnilib -O3 -dynamiclib  -I$JAVA_HOME/include -framework JavaVM -framework Cocoa -read_only_relocs suppress -single_module
