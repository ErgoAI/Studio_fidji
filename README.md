To build Ergo Studio:

* make sure you have JDK 1.8 and ant installed
* checkout the whole tree
* Copy build.xml and edit the copy to indicate where XSB and Ergo are sitting and also the "bin.dir" property.
* ant -f build-copy.xml, where build-copy.xml is the edited copy of build.xml discussed above.

This should build ergoStudio.jar.

About subdirectories nearby:

* interprologForJDK/ is where all source code is for Ergo Studio and its subsets - Prolog Studio and Java bridge
* interprolog/ is where the InterProlog Java bridge sources will be extracted into by bin/ip.sh

No longer here:
** AutoComplete-master/, juniversalchardet/, RSTAUI-master/ and RSyntaxTextArea-master-SOME_DATE/ are open source ingredients:
** ... see comments in externalJars/
