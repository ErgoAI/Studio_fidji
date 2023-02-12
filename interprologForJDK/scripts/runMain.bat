echo off
CALL windowsVariables.bat
%JAVA_BIN%\java -Djava.library.path=%XSB_BIN_DIRECTORY% -classpath %CLASSPATH%;../ergoStudio.jar %1
