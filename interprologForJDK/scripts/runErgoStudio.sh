. unixVariables.sh
# ${JAVA_BIN}/java -Xss2M -Xmx128M -Xdock:name=Fidji -DFLORADIR=/Users/mc/Dropbox/flora2 -classpath ${CLASSPATH}:../fidji.jar com.declarativa.fiji.FidjiSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
# ${JAVA_BIN}/java -Xss2M -Xmx128M -Xdock:name=Fidji -DFLORADIR=/Users/mc/coherent-engine-pre-syntax-changes/flora2 -classpath ${CLASSPATH}:../fidji.jar com.declarativa.fiji.FidjiSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
# ${JAVA_BIN}/java -Xss2M -Xmx128M -Xdock:name=Fidji -DFLORADIR=/Users/mc/coherent-engine/flora2 -classpath ${CLASSPATH}:../fidji.jar com.declarativa.fiji.FidjiSubprocessEngineWindow $1 ${XSB_BIN_DIRECTORY}/xsb
# For icon add -Xdock:icon=PathToImage
${JAVA_BIN}/java -Xss2M -Xmx1800M -Xdock:name="Ergo Suite" -Djava.net.preferIPv4Stack=true -DFLORADIR=/Users/mc/coherent-engine/flora2 -classpath ${CLASSPATH}:../ergoStudio.jar com.declarativa.fiji.FijiSubprocessEngineWindow -printlog $1 ${XSB_BIN_DIRECTORY}/xsb
