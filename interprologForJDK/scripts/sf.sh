# Strip proprietary code out of Fidji
echo 'Obtaining InterPrologStudio...'
cp ../ergoStudio.jar ../interPrologStudio.jar
zip -dq ../interPrologStudio.jar com/coherentknowledge/* 
echo '  To obtain InterProlog: cd .. ; ant interprologJar'
