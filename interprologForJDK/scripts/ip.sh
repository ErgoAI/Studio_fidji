# Copy all InterProlog code out of Fidji into its local repository working directory, ready for committing to Github
# Couldn't put orphan branches and recipe at http://alexgaudio.com/2011/07/24/sharing-part-your-repo-github.html to advantageous use
# This hand executed script should be executed ONLY with a CLEAN working area in the github repo!
# External interprolog pull requests should be digested there; when they start happening and changes start flowing into this private dir, the bulk of this script will
# likely use rsync instead. Eventually, interprolog should be removed out of Studio and made a separate git repository altogether, but I like the present holistic approach;-)

. unixVariables.sh  # make sure this is current, e.g. by executing b.sh before
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/examples
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/examples/SudokuPuzzles
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/gui
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/gui/images
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/remote
mkdir -p ../../../interprolog_github/src/main/java/com/declarativa/interprolog/util
mkdir -p ../../../interprolog_github/src/main/java/com/xsb/interprolog
mkdir -p ../../../interprolog_github/src/test/java/com/declarativa/interprolog
mkdir -p ../../../interprolog_github/src/test/java/com/xsb/interprolog
mkdir -p ../../../interprolog_github/externalJars

cp -p ../src/com/declarativa/interprolog/*.java ../src/com/declarativa/interprolog/*.P  ../../../interprolog_github/src/main/java/com/declarativa/interprolog
cp -p ../src/com/declarativa/interprolog/examples/*.java ../src/com/declarativa/interprolog/examples/*.P  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/examples
cp -p ../src/com/declarativa/interprolog/examples/SudokuPuzzles/*.txt  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/examples/SudokuPuzzles
cp -p ../src/com/declarativa/interprolog/gui/*.java ../src/com/declarativa/interprolog/gui/*.P  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/gui
cp -p ../src/com/declarativa/interprolog/gui/images/*.gif  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/gui/images
cp -p ../src/com/declarativa/interprolog/remote/*.java  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/remote
cp -p ../src/com/declarativa/interprolog/util/*.java  ../../../interprolog_github/src/main/java/com/declarativa/interprolog/util

cp -p ../src/com/xsb/interprolog/*.java ../../../interprolog_github/src/main/java/com/xsb/interprolog
cp -p ../test/com/xsb/interprolog/*.java ../../../interprolog_github/src/test/java/com/xsb/interprolog
cp -p ../test/com/declarativa/interprolog/*.java ../../../interprolog_github/src/test/java/com/declarativa/interprolog

cp -p ../externalJars/junit-4.5.jar ../externalJars/junit-README.txt ../../../interprolog_github/externalJars
cp -p ../build_interprolog.xml ../README_interprolog_at_githubt.txt ../../../interprolog_github
cp -p -R ../otherInterPrologDoc ../../../interprolog_github

cp ../pomForInterprolog.xml ../../../interprolog_github/pom.xml

cd ../../../interprolog_github
ant -buildfile build_interprolog.xml -DXSB_BIN_DIRECTORY=${XSB_BIN_DIRECTORY}
echo "***Running SWI example:"
java -cp interprolog.jar com.declarativa.interprolog.examples.HelloWorldSWI
echo "***Running XSB example:"
java -cp interprolog.jar com.declarativa.interprolog.examples.HelloWorldXSB
