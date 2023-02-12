echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -Djava.library.path=%XSB_BIN_DIRECTORY% -classpath ..\ergoStudio.jar com.xsb.interprolog.NativeEngineWindow %1 %XSB_BIN_DIRECTORY%
REM pause
echo on
