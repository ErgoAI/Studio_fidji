. unixVariables.sh
${JAVA_BIN}/java -Xss2M -Xmx128M -Xdock:name=Fiji -classpath ${CLASSPATH}:../interPrologStudio.jar com.declarativa.fiji.FijiSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
