. unixVariables.sh
# ${JAVA_BIN}/java -classpath ${CLASSPATH}:../ergoStudio.jar com.declarativa.interprolog.gui.XSBSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
${JAVA_BIN}/java -classpath ${CLASSPATH}:../interprolog.jar com.declarativa.interprolog.gui.XSBSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
