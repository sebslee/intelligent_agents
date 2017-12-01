#!/bin/bash
echo "-I-:Compiling the agent..."
cd ../genius/ 
javac -cp negosimulator.jar ../src/group26/Agent26.java
if [ $? -eq 0 ]; then 
echo "-I-: Compilation succesful ..."
else 
    echo "-E-: Compilation failed exiting...";
    exit
fi
cd ../src/
echo "-I-: Removing old jar file ..."
rm group26.jar
echo "-I-: Creating new jar ..."
jar cvf group26.jar -c group26
echo "-I-: Adding files ..."
jar uvf group26.jar group26/
if [ $? -eq 0 ]; then 
echo "-I-: Jar file generated succesfully check contents please ..."
else 
    echo "-E-: Fail exiting...";
    exit
fi
jar tvf group26.jar
echo "-I- : Exited succesfully .."




