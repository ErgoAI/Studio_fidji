. unixVariables.sh
SWI_BIN_DIRECTORY=/opt/local/bin
${JAVA_BIN}/java -classpath ${CLASSPATH}:../interprolog.jar com.declarativa.interprolog.gui.SWISubprocessEngineWindow $1 ${SWI_BIN_DIRECTORY}
