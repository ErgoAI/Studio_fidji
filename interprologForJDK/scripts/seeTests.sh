CURRENT_DIRECTORY=`pwd`
cd ..
ant testreport
open testresults/html/index.html
cd $CURRENT_DIRECTORY
