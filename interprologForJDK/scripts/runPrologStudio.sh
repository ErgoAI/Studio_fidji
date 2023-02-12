. unixVariables.sh
${JAVA_BIN}/java -Xdock:name="InterProlog Studio" -classpath ${CLASSPATH}:../interPrologStudio.jar com.declarativa.fiji.FijiSubprocessEngineWindow -printlog $1 $2 ${XSB_BIN_DIRECTORY}/xsb
