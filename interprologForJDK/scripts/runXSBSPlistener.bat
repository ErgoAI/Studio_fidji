echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -classpath ..\interprolog.jar com.declarativa.interprolog.gui.XSBSubprocessEngineWindow %XSB_BIN_DIRECTORY%\xsb %1
rem pause
echo on
