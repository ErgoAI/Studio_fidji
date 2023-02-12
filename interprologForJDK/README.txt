This whole folder tree is the "Fidji project", and includes software copyrighted by 
Coherent Knowledge Systems LLC, USA, and/or InterProlog Consulting (Renting Point Lda.), Portugal
More details at the top of the ant build.xml file

You can build several Ergo Studio and several subset products using that ant file... 
... or alternatively build (dev only) versions of ErgoStudio using Eclipse (after fixing paths in .project, .classpath).

Using Eclipse to generate customer products requires further configuring to make sure logic source files do not end up jar-ed;
also, the "build version" fetched from git is not available.
Please see ant file. 

In conclusion: if you're shipping something to someone, use the ant build.