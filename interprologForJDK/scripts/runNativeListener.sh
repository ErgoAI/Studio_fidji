. unixVariables.sh
${JAVA_BIN}/java -Xcheck:jni -Djava.library.path=${XSB_BIN_DIRECTORY} -classpath ${CLASSPATH}:../ergoStudio.jar com.xsb.interprolog.NativeEngineWindow $1 ${XSB_BIN_DIRECTORY}
