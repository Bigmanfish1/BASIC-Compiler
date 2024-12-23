import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class CodeGeneration {
    private Map<Integer, SymbolTable.VariableInfo> symbolTable = new HashMap<>();
    private int newVarCounter;
    private int newLabelCounter;
    private String temp;

    public CodeGeneration(Map<Integer, SymbolTable.VariableInfo> sT) {
        newVarCounter = 1;
        newLabelCounter = 1;
        this.symbolTable = sT;
    }

    public String translate(SyntaxTree.Node node, String place) throws Exception {
        String result = "";

        switch (node.value) {
            case "PROG":
                String algoCode = translate(node.children.get(2), null);
                String funcCode = translate(node.children.get(3), null);
                result += algoCode + "\nSTOP\n" + funcCode;
                break;

            case "ALGO":
                result += translate(node.children.get(1), null);
                break;

            case "INSTRUC":
                if (node.children.get(0).value.equals("e")) {
                    result += "REM END";
                } else {
                    for (SyntaxTree.Node child : node.children) {
                        result += translate(child, null);
                    }
                }
                break;

            case "COMMAND":
                if (node.children.get(0).value.equals("skip")) {
                    result += "REM DO NOTHING\n";
                } else if (node.children.get(0).value.equals("halt")) {
                    result += " STOP \n";
                } else if (node.children.get(0).value.equals("print")) {
                    result += "PRINT " + translate(node.children.get(1), null) + "\n";
                } else if(node.children.get(0).value.equals("return")){
                    result += translate(node.children.get(1), temp) + "\n";
                }else {
                    result += translate(node.children.get(0), null);
                }
                break;

            case "ASSIGN":
                if (node.children.size() == 2) {
                    result += "INPUT " + translate(node.children.get(0), null) + "\n";
                } else if (node.children.size() == 3) {
                    String place1 = newVar();
                    SymbolTable.VariableInfo varInfo = symbolTable.get(node.children.get(0).children.get(0).unid);
                    String x = varInfo.uniqueName;
                    result += translate(node.children.get(2), place1) + "\n" + x + " := " + place1 + "\n";
                }
                break;

            case "ATOMIC":
                if (node.children.get(0).value.equals("VNAME")) {
                    result += translate(node.children.get(0), place);
                } else if (node.children.get(0).value.equals("CONST")) {
                    result += translate(node.children.get(0), place);
                }

                break;
            case "VNAME":
                int originalUnid = node.children.get(0).unid;
                SymbolTable.VariableInfo varInfo = symbolTable.get(originalUnid);
                if(place != null){
                    result += place + " := " + varInfo.uniqueName;
                }else{
                    result += varInfo.uniqueName;
                }
                
                break;

            case "CONST":
                String constValue = node.children.get(0).value;
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                if (pattern.matcher(constValue).matches()) {
                    
                    if(place != null){
                        result += place + " := " + constValue + " ";
                    }else{
                        result += " " + constValue + " ";
                    }
                } else {
                    if(place != null){
                        result += place +  " := " + constValue + " ";
                    }else{
                        result += " " + constValue + " ";
                    }
                }
                break;

            case "TERM":
                result += translate(node.children.get(0), place);
                break;
            case "CALL":
                SymbolTable.VariableInfo variableInfo = symbolTable.get(node.children.get(0).children.get(0).unid);
                String arg1 = translate(node.children.get(2), null);
                String arg2 = translate(node.children.get(4), null);
                String arg3 = translate(node.children.get(6), null);
                String newFunctionName = variableInfo.uniqueName;
                result += place + " := CALL_" + newFunctionName + "(" + arg1 + "," + arg2 + "," + arg3 + ")";
                break;

            case "OP":
                if (node.children.get(0).value.equals("UNOP")) {
                    String place1 = newVar();
                    String unopName = translate(node.children.get(0), null);
                    String code1 = translate(node.children.get(2), place1);
                    result += code1 + place + ":=" + unopName + "(" + place1 + ")";
                } else if (node.children.get(0).value.equals("BINOP")) {
                    String place1 = newVar();
                    String place2 = newVar();
                    String code1 = translate(node.children.get(2), place1);
                    String code2 = translate(node.children.get(4), place2);
                    String binopName = translate(node.children.get(0), null);

                    result += code1 + "\n" + code2 + "\n" + place + ":=" + place1 + binopName + place2;
                }
                break;
            case "UNOP":
                if (node.children.get(0).value.equals("sqrt")) {
                    result += "SQR";
                }else if(node.children.get(0).value.equals("not")){
                    result += "NOT";
                }
                break;

            case "BINOP":
                switch (node.children.get(0).value) {
                    case "eq":
                        return "=";
                    case "grt":
                        return ">";
                    case "add":
                        return "+";
                    case "sub":
                        return "-";
                    case "mul":
                        return "*";
                    case "div":
                        return "/";
                    default:
                        throw new IllegalArgumentException(
                                "Unsupported binary operator: " + node.children.get(0).value);
                }

            case "ARG":
                result += translate(node.children.get(0), place);
                break;

            case "FNAME":
                SymbolTable.VariableInfo fnameInfo = symbolTable.get(node.children.get(0).unid);
                result += fnameInfo.uniqueName;
                break;

            case "BRANCH":
                if(node.children.get(1).children.get(0).value.equals("COMPOSIT")){
                    String label1 = newLabel();
                    String label2 = newLabel();
                    String label3 = newLabel();

                    String code1 = translateCond(node.children.get(1).children.get(0), label1, label2);
                    String code2 = translate(node.children.get(3), null);
                    String code3 = translate(node.children.get(5), null);
                    result += code1 + "LABEL " + label1 + "\n" + code2 + "\nGOTO " + label3 + "\nLABEL " + label2 + "\n" + code3 + "\nLABEL " + label3 + "\n";
                }else{
                    String label1 = newLabel();
                    String label2 = newLabel();
                    String label3 = newLabel();

                    String code1 = translateCond(node.children.get(1).children.get(0), label1, label2);
                    String code2 = translate(node.children.get(3), null);
                    String code3 = translate(node.children.get(5), null);
                    result += code1 + "LABEL " + label1 + "\n" + code2 + "\n GOTO " + label3 + "\nLABEL " + label2 + "\n" + code3 + "\nLABEL " + label3 + "\n"; 
                }
                break;
            default:
                result += "";
                break;
        }

        return result;

    }

    public String translateCond(SyntaxTree.Node condNode, String labelTrue, String labelFalse) throws Exception {
        String result = "";

        switch (condNode.value) {
            case "SIMPLE":
                String place1 = newVar();
                String place2 = newVar();
                String code1 = translate(condNode.children.get(2), place1);  
                String code2 = translate(condNode.children.get(4), place2);
                String relop = translate(condNode.children.get(0), null);    

                result += code1 + "\n" + code2 + "\n";
                result += "IF " + place1 + " " + relop + " " + place2 + " THEN " + labelTrue + " ELSE " + labelFalse + "\n";
                break;

            case "COMPOSIT":
                if (condNode.children.get(0).value.equals("UNOP")) {
                    if(condNode.children.get(0).children.get(0).value.equals("not")){
                        result += translateCond(condNode.children.get(2), labelFalse, labelTrue);
                    }
                    
                } else if (condNode.children.get(0).value.equals("BINOP")) {
                    if (condNode.children.get(0).children.get(0).value.equals("and")) {
                        // COMPOSIT ::= SIMPLE1 && SIMPLE2
                        String labelMid = newLabel();
                        String codeS1 = translateCond(condNode.children.get(2), labelMid, labelFalse);
                        String codeS2 = translateCond(condNode.children.get(4), labelTrue, labelFalse);
                        result += codeS1 + " LABEL " + labelMid + "\n" + codeS2;
                    } else if (condNode.children.get(0).children.get(0).value.equals("or")) {
                        // COMPOSIT ::= SIMPLE1 || SIMPLE2
                        String labelMid = newLabel();
                        String codeS1 = translateCond(condNode.children.get(2), labelTrue, labelMid);
                        String codeS2 = translateCond(condNode.children.get(4), labelTrue, labelFalse);
                        result += codeS1 + " LABEL " + labelMid + "\n" + codeS2;
                        
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown condition type: " + condNode.value);
        }

        return result;
    }

    public String translateFunction(SyntaxTree.Node node, String functionName, String[] parameters) throws Exception {
        String result = "";

        switch (node.value) {
            case "FUNCTIONS":
                if(node.children.size() == 2)
                {
                    String str1 = translateFunction(node.children.get(0), functionName, parameters);
                    String str2 = translateFunction(node.children.get(1), functionName, parameters);
                    result += str1 + "\nSTOP\n" + str2;
                    
                }
                else if(node.children.size() == 1)
                {
                    result += "REM END\n";
                    
                }
                break;

            case "SUBFUNCS":
                result += translateFunction(node.children.get(0), functionName, parameters);
                break;
            case "BODY":
            {
                String pCode = translate(node.children.get(0), null);
                String aCode = translate(node.children.get(2), null);
                String eCode = translate(node.children.get(3), null);
                String sCode = translateFunction(node.children.get(4), functionName, parameters);
                result += pCode + "\n" + aCode + "\n" + eCode + sCode;
                break;

            }
            case "EPILOG":
                result += "REM END\n";
                break;
            case "PROLOG":
                result += "REM BEGIN\n";
                break;
            case "DECL":
                result += translateFunction(node.children.get(0), functionName, parameters);
                result += translateFunction(node.children.get(1), functionName, parameters);
                break;
            case "HEADER":
                String str1 = node.children.get(3).children.get(0).value +" := "+ parameters[0];
                String str2 = node.children.get(5).children.get(0).value +" := "+ parameters[1];
                String str3 = node.children.get(7).children.get(0).value +" := "+ parameters[2];
                result += str1 + "\n" + str2 + "\n" + str3;
                break;
            default:
                result += "";
                break;
        }

        return result;

    }

    public void processFunctionCalls(String fileName,SyntaxTree.Node root) {
        String result = "";
        do {
        try {
            if(result.length() > 0){
                fileName = "Phase5B.txt";
                result = "";
            }
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                //check if it's a function call
                if(data.contains("CALL_")){
                    String[] parts = data.split(" := ");
                    temp = parts[0];
                    String pattern = "CALL_([a-zA-Z0-9_]+)\\((.*?)\\)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(parts[1]);

                    if (m.find()) {
                        String functionName = m.group(1);
                        String[] parameters = m.group(2).split(",");
                        // System.out.println("Function Name: " + functionName);
                        // System.out.println("Parameters: " + Arrays.toString(parameters));
                        int unid = -1;
                        for (Map.Entry<Integer, SymbolTable.VariableInfo> entry : symbolTable.entrySet()) {
                            if (entry.getValue().uniqueName.equals(functionName)) {
                                unid = entry.getKey();
                                break;
                            }
                        }
                        SyntaxTree.Node node = findFunctionDeclaration(root,unid);
                    
                        try {
                            result += translateFunction(node, functionName, parameters);
                           
                            
                            
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        System.out.println("No match found.");
                    }
                }
                else
                {
                    result += data + "\n";
                }
                
            }
            
           
            try {
                FileWriter myWriter = new FileWriter("Phase5B.txt");
                myWriter.write(result);
                myWriter.close();
                
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        } 
        while (result.contains("CALL_"));
        System.out.println(result);
        System.out.println("Successfully wrote to Phase5B.txt");
    }

    public SyntaxTree.Node findFunctionDeclaration(SyntaxTree.Node node, int unid) {
        //check for HEADER and the node.children.get(1).unid == unid
        if(node.value.equals("HEADER") && node.children.get(1).children.get(0).unid == unid){
            return node.parent;
        }
        for (SyntaxTree.Node child : node.children) {
            SyntaxTree.Node result = findFunctionDeclaration(child, unid);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public String newVar() {
        return "t" + newVarCounter++;
    }

    public String newLabel() {
        return "l" + newLabelCounter++;
    }
}
