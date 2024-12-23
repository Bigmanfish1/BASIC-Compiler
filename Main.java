import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
public class Main {

    public static void main(String[] args) throws SyntaxException, Exception {
        Lexer lexer;
        lexer = new Lexer("input.txt");
        lexer.createXML();

        Parser lexerParser = new Parser();
        String xmlFilePath = "tokens.xml";

        List<Token> tokens = lexerParser.parseXmlInput(xmlFilePath);

        lexerParser.parse(tokens);

        ScopeAnalysis sA = new ScopeAnalysis();
        SyntaxTree syntaxTree = sA.parseXMLToSyntaxTree("syntax_tree.xml");
        
        sA.analyze(syntaxTree.getRoot());
        TypeChecker tC = new TypeChecker(sA.getLargeSymbolTable());
        boolean type = tC.typeCheck(syntaxTree.getRoot());
        //syntaxTree.printSyntaxTree();
        System.out.println("Type check: " + type);
        if(!type)
        {
            throw new Exception("Type check failed");   
        }
        Map<Integer, SymbolTable.VariableInfo> sT = tC.getLargeSymbolTable();
        String symbols = tC.printSymbolTable();
        System.out.println(symbols);
        try {
            FileWriter myWriter = new FileWriter("symbol_table.txt");
            myWriter.write(symbols);
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        CodeGeneration intermediateCode = new CodeGeneration(sT);
        String result = intermediateCode.translate(syntaxTree.getRoot().children.get(0), null);
        //save this result in a txt file
        try {
            FileWriter myWriter = new FileWriter("Phase5A.txt");
            myWriter.write(result);
            myWriter.close();
            //System.out.println(result);
            System.out.println("Successfully wrote to Phase5A.txt\n");

        } catch (IOException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
        }

        //intermediateCode.processFunctionCalls("Phase5A.txt",syntaxTree.getRoot().children.get(0));

        TargetCodeGeneration aCG = new TargetCodeGeneration(sT);
        String result1 = aCG.generateBasicCode(syntaxTree.getRoot().children.get(0));
        System.err.println(result1);
        try {
            FileWriter myWriter = new FileWriter("Phase5B.txt");
            myWriter.write(result1);
            myWriter.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
         
    }
}