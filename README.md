# BASIC-Compiler

# Compiler Project

## Assumptions
1. The file name of the input source code must be named input.txt
2. The source code must have a whitespace between all tokens there is an example input.txt in the submission zip. The use of spaces was specified in the project spec.
3. The '< input' token is the same as it is in the grammar which is '< input' and not '<_input' for example.
4. The output for phase 1 is in the tokens.xml file
5. The output for phase 2 is in the syntax_tree.xml file
6. The output for phase 3 and 4 is in the symbol_table.txt file
7. The output for phase 5a is in the Phase5A.txt file
8. The output for phase 5b is in the Phase5B.txt file
9. The BASIC emulator that was used is the TutorialsPoint Online Ya Basic Compiler, please make use of this emulator when testing our BASIC code, which can be found at the following link: https://www.tutorialspoint.com/execute_basic_online.php 
10. We use a max recursion depth of 20 

## How to create the compiler.jar
1. Ensure you have an input.txt in the current directory
2. Create a manifest.MF file and place the following text in the file: 'Main-Class: Main ' and PUT A NEWLINE AFTER THE TEXT
3. compile the code: javac Main.java
4. create the jar file: jar cfe compiler.jar Main *.class
5. run the jar file: java -jar compiler.jar

## How to run source code
1. Ensure you have an input.txt in the current directory
2. javac Main.java
3. java Main

## How to run the compiler.jar
1. Ensure you have an input.txt in the current directory
2. java -jar compiler.jar
