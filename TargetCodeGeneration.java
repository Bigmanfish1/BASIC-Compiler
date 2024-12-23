import java.util.*;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class TargetCodeGeneration {
    private Map<Integer, SymbolTable.VariableInfo> symbolTable = new HashMap<>();
    private int lineNumber = 10;
    private Map<String, Integer> functionLineNumbers = new HashMap<>();
    private List<String> localVars = new ArrayList<>();
    private List<String> paramVars = new ArrayList<>();
    private int maxIterations = 20;

    public TargetCodeGeneration(Map<Integer, SymbolTable.VariableInfo> sT) {
        this.symbolTable = sT;
    }

    public String translateToBasic(SyntaxTree.Node node) {
        String result = "";

        switch (node.value) {
            case "PROG":
                result += lineNumber + " DIM M(7, " + maxIterations + ")\n";
                lineNumber += 10;
                result += lineNumber + " LET f = 0\n";
                lineNumber += 10;
                String globVars = translateToBasic(node.children.get(1));
                String algoCode = translateToBasic(node.children.get(2));
                int stopNum = lineNumber;
                lineNumber += 10;
                String funcCode = translateToBasic(node.children.get(3));
                result += globVars + "\n" + algoCode + "\n" + stopNum + " END\n" + funcCode;
                break;

            case "GLOBVARS":
                for (SyntaxTree.Node child : node.children) {
                    if (child.value.equals("VNAME")) {
                        SymbolTable.VariableInfo varInfo = symbolTable.get(child.children.get(0).unid);
                        result += lineNumber + " LET " + varInfo.uniqueName + " = 0\n";
                        lineNumber += 10;
                    } else if (child.value.equals("GLOBVARS")) {
                        result += translateToBasic(child);
                    }
                }
                break;
            case "ALGO":
                result += translateToBasic(node.children.get(1)) + "\n";
                break;

            case "INSTRUC":
                if (node.children.get(0).value.equals("e")) {
                    result += lineNumber + " REM END";
                    lineNumber += 10;
                } else {
                    for (SyntaxTree.Node child : node.children) {
                        result += translateToBasic(child);
                    }
                }
                break;

            case "COMMAND":
                if (node.children.get(0).value.equals("skip")) {
                    result += lineNumber + " REM DO NOTHING\n";
                    lineNumber += 10;
                } else if (node.children.get(0).value.equals("halt")) {
                    result += lineNumber + " STOP\n";
                    lineNumber += 10;
                } else if (node.children.get(0).value.equals("print")) {
                    result += lineNumber + " PRINT " + translateToBasic(node.children.get(1)) + "\n";
                    lineNumber += 10;
                } else if (node.children.get(0).value.equals("return")) {
                    if (node.children.size() > 1) {
                        // Return a value (store it in M(0, f))
                        String returnValue = translateToBasic(node.children.get(1));
                        result += lineNumber + " LET f = f - 1\n";
                        lineNumber += 10;
                        result += lineNumber + " LET M(0, f) = " + returnValue + "\n";
                        lineNumber += 10;
                    }
                    result += lineNumber + " RETURN\n";
                    lineNumber += 10;
                } else {
                    result += translateToBasic(node.children.get(0));
                }
                break;

            case "ASSIGN":
                if (node.children.size() == 2) {
                    result += lineNumber + " INPUT " + translateToBasic(node.children.get(0)) + "\n";
                    lineNumber += 10;
                } else if (node.children.size() == 3) {
                    if (node.children.get(2).children.get(0).value.equals("CALL")) {
                        SymbolTable.VariableInfo variableInfo = symbolTable
                                .get(node.children.get(2).children.get(0).children.get(0).children.get(0).unid);
                        String functionName = variableInfo.uniqueName;
                        String arg1 = translateToBasic(node.children.get(2).children.get(0).children.get(2));
                        String arg2 = translateToBasic(node.children.get(2).children.get(0).children.get(4));
                        String arg3 = translateToBasic(node.children.get(2).children.get(0).children.get(6));
                        result += saveCurrLocalVariables();
                        result += lineNumber + " LET f = f + 1\n";
                        lineNumber += 10;
                        result += lineNumber + " IF f > " + maxIterations + " THEN\n";
                        lineNumber += 10;
                        result += lineNumber + " LET f = f - 1\n";
                        lineNumber += 10;
                        result += lineNumber + " RETURN\n";
                        lineNumber += 10;
                        result += lineNumber + " END IF\n";
                        lineNumber += 10;
                        result += lineNumber + " LET M(1, f) = " + arg1 + "\n";
                        lineNumber += 10;
                        result += lineNumber + " LET M(2, f) = " + arg2 + "\n";
                        lineNumber += 10;
                        result += lineNumber + " LET M(3, f) = " + arg3 + "\n";
                        lineNumber += 10;
                        result += lineNumber + " GOSUB " + functionName + "\n";
                        lineNumber += 10;
                        if (!variableInfo.type.equals("n")) {
                            result += lineNumber + " LET f = f - 1\n";
                            lineNumber += 10;
                        }

                        result += restoreLocalVariables();
                    }
                    SymbolTable.VariableInfo varInfo = symbolTable.get(node.children.get(0).children.get(0).unid);
                    String x = "";
                    if(varInfo.type.equals("t")){
                        x = varInfo.uniqueName + "$";
                    }else{
                        x = varInfo.uniqueName;
                    }

                    result += lineNumber + " LET " + x + " = " + translateToBasic(node.children.get(2)) + "\n";
                    lineNumber += 10;
                }
                break;

            case "ATOMIC":
                if (node.children.get(0).value.equals("VNAME")) {
                    result += translateToBasic(node.children.get(0));
                } else if (node.children.get(0).value.equals("CONST")) {
                    result += translateToBasic(node.children.get(0));
                }

                break;
            case "VNAME":
                int originalUnid = node.children.get(0).unid;
                SymbolTable.VariableInfo varInfo = symbolTable.get(originalUnid);
                if(varInfo.type.equals("t")){
                    result += varInfo.uniqueName + "$";
                }else{
                    result += varInfo.uniqueName;
                }
                
                break;

            case "CONST":
                String constValue = node.children.get(0).value;
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                if (pattern.matcher(constValue).matches()) {
                    result += constValue;
                } else {
                    result += constValue;
                }
                break;

            case "TERM":
                result += translateToBasic(node.children.get(0));
                break;

            case "CALL":

                SymbolTable.VariableInfo variableInfo = symbolTable.get(node.children.get(0).children.get(0).unid);
                String functionName = variableInfo.uniqueName;
                // if type is num
                if (symbolTable.get(variableInfo.unid).type.equals("n")) {
                    result += " M(0,f)\n";
                    lineNumber += 10;
                } else {
                    String arg1 = translateToBasic(node.children.get(2));
                    String arg2 = translateToBasic(node.children.get(4));
                    String arg3 = translateToBasic(node.children.get(6));
                    result += saveCurrLocalVariables();
                    result += lineNumber + " LET f = f + 1\n";
                    lineNumber += 10;
                    result += lineNumber + " IF f > " + maxIterations + " THEN\n";
                    lineNumber += 10;
                    result += lineNumber + " LET f = f - 1\n";
                    lineNumber += 10;
                    result += lineNumber + " RETURN\n";
                    lineNumber += 10;
                    result += lineNumber + " END IF\n";
                    lineNumber += 10;
                    result += lineNumber + " LET M(1, f) = " + arg1 + "\n";
                    lineNumber += 10;
                    result += lineNumber + " LET M(2, f) = " + arg2 + "\n";
                    lineNumber += 10;
                    result += lineNumber + " LET M(3, f) = " + arg3 + "\n";
                    lineNumber += 10;
                    result += lineNumber + " GOSUB " + functionName + "\n";
                    lineNumber += 10;
                    if (!variableInfo.type.equals("n")) {
                        result += lineNumber + " LET f = f - 1\n";
                        lineNumber += 10;
                    }
                    result += restoreLocalVariables();
                }

                break;

            case "OP":
                if (node.children.get(0).value.equals("UNOP")) {
                    String unopName = translateToBasic(node.children.get(0));
                    String arg = translateToBasic(node.children.get(2));
                    result += unopName + "(" + arg + ")";
                } else if (node.children.get(0).value.equals("BINOP")) {
                    String leftArg = translateToBasic(node.children.get(2));
                    String rightArg = translateToBasic(node.children.get(4));
                    String binopName = translateToBasic(node.children.get(0));
                    result += leftArg + " " + binopName + " " + rightArg;
                }
                break;
            case "UNOP":
                if (node.children.get(0).value.equals("sqrt")) {
                    result += "SQR";
                } else if (node.children.get(0).value.equals("not")) {
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
                result += translateToBasic(node.children.get(0));
                break;

            case "FNAME":
                SymbolTable.VariableInfo fnameInfo = symbolTable.get(node.children.get(0).unid);
                result += fnameInfo.uniqueName;
                break;

            case "BRANCH":
                String code1 = translateToBasic(node.children.get(1));
                result += lineNumber + " IF " + code1 + " THEN\n";
                lineNumber += 10;
                String code2 = translateToBasic(node.children.get(3));
                result += code2;
                result += lineNumber + " ELSE\n";
                lineNumber += 10;
                String code3 = translateToBasic(node.children.get(5));
                result += code3;
                result += lineNumber + " END IF\n";
                lineNumber += 10;
                break;
            case "COND":
                result += translateToBasic(node.children.get(0));
                break;
            case "SIMPLE":
                String leftSide = translateToBasic(node.children.get(2));
                String rightSide = translateToBasic(node.children.get(4));
                String relop = translateToBasic(node.children.get(0));
                result += leftSide + " " + relop + " " + rightSide;
                break;
            case "COMPOSIT":
                if (node.children.get(0).value.equals("UNOP")) {
                    String unop = translateToBasic(node.children.get(0));
                    String cond = translateToBasic(node.children.get(2));
                    result += unop + "(" + cond + ")";
                } else {
                    String leftCond = translateToBasic(node.children.get(2));
                    String rightCond = translateToBasic(node.children.get(4));
                    String binop = translateToBasic(node.children.get(0));
                    result += leftCond + " " + binop + " " + rightCond;
                }

                break;

            case "FUNCTIONS":
                for (SyntaxTree.Node child : node.children) {
                    result += translateToBasic(child);
                }
                break;

            case "DECL":
                result += translateToBasic(node.children.get(0));
                result += translateToBasic(node.children.get(1));
                break;

            case "BODY":
                result += translateToBasic(node.children.get(0));
                result += translateToBasic(node.children.get(1));
                result += translateToBasic(node.children.get(2));
                result += translateToBasic(node.children.get(3));
                break;

            case "EPILOG":

                SyntaxTree.Node fnameReturnInfo = node.parent.parent.children.get(0).children.get(0).children.get(0);
                SymbolTable.VariableInfo uniqueFname = symbolTable
                        .get(node.parent.parent.children.get(0).children.get(1).children.get(0).unid);
                if (fnameReturnInfo.value.equals("num")) {
                    // result += lineNumber + " FN" + uniqueFname.uniqueName + " = M(0, f)\n";
                } else {
                    // result += lineNumber + " LET f = f - 1\n";
                    // lineNumber += 10;
                    result += lineNumber + " RETURN\n";
                }
                lineNumber += 10;
                break;

            case "LOCVARS":// check this
                // result += lineNumber + " LET M(4, f) = 0\n";
                // lineNumber += 10;
                // result += lineNumber + " LET M(5, f) = 0\n";
                // lineNumber += 10;
                // result += lineNumber + " LET M(6, f) = 0\n";
                // lineNumber += 10;
                localVars = new ArrayList<>();
                result += saveLocalVariables(node);
                break;

            case "HEADER":
                // Extract function type and name
                SymbolTable.VariableInfo fnameInformation = symbolTable.get(node.children.get(1).children.get(0).unid);
                String functionName2 = fnameInformation.uniqueName;

                if (node.children.get(0).children.get(0).value.equals("num")) {
                    functionLineNumbers.put(functionName2, lineNumber);
                    result += lineNumber + " REM DEF FN" + functionName2 + "(a1, a2, a3)\n";
                    lineNumber += 10;
                    // result += lineNumber + " LET f = f + 1\n";
                    // lineNumber += 10;
                    // result += lineNumber + " IF f > 2 THEN\n";
                    // lineNumber += 10;
                    // result += lineNumber + " LET f = f - 1\n";
                    // lineNumber += 10;
                    // result += lineNumber + " RETURN\n";
                    // lineNumber += 10;
                    // result += lineNumber + " END IF\n";
                    // lineNumber += 10;

                    // result += lineNumber + " LET M(1, f) = a1\n";
                    // lineNumber += 10;
                    // result += lineNumber + " LET M(2, f) = a2\n";
                    // lineNumber += 10;
                    // result += lineNumber + " LET M(3, f) = a3\n";
                    // lineNumber += 10;
                    paramVars = new ArrayList<>();
                    result += linkParameterVariables(node);
                } else if (node.children.get(0).children.get(0).value.equals("void")) {
                    functionLineNumbers.put(functionName2, lineNumber);
                    result += lineNumber + " REM DEF FN" + functionName2 + "(a1, a2, a3)\n";
                    lineNumber += 10;
                    // result += lineNumber + " LET f = f + 1\n";
                    // lineNumber += 10;
                    // result += lineNumber + " IF f > 2 THEN\n";
                    // lineNumber += 10;
                    // result += lineNumber + " LET f = f - 1\n";
                    // lineNumber += 10;
                    // result += lineNumber + " RETURN\n";
                    // lineNumber += 10;
                    // result += lineNumber + " END IF\n";
                    // lineNumber += 10;
                    paramVars = new ArrayList<>();
                    result += linkParameterVariables(node);
                }
                break;
            default:
                result += "";
                break;
        }

        return result;
    }

    public String generateBasicCode(SyntaxTree.Node root) {
        String basicCode = translateToBasic(root);
        // look for GOSUB then replace the function name with line numbers in the
        // basicCode like where the is a GOSUB in the line

        for (Map.Entry<String, Integer> entry : functionLineNumbers.entrySet()) {
            String functionName = entry.getKey();
            int lineNumber = entry.getValue();
            basicCode = basicCode.replaceAll("GOSUB " + functionName, "GOSUB " + lineNumber);
        }

        return basicCode;

    }

    private void getLocalVariables(SyntaxTree.Node functionBody) {
        // Traverse LOCVARS node to collect variable names
        if (functionBody != null && functionBody.value.equals("LOCVARS")) {
            for (SyntaxTree.Node child : functionBody.children) {
                if (child.value.equals("VNAME")) {
                    SymbolTable.VariableInfo varInfo = symbolTable.get(child.children.get(0).unid);
                    localVars.add(varInfo.uniqueName);
                }
            }
        }
    }

    private String saveLocalVariables(SyntaxTree.Node functionBody) {
        StringBuilder saveVars = new StringBuilder();
        getLocalVariables(functionBody);

        for (int i = 0; i < localVars.size(); i++) {
            saveVars.append(lineNumber + " LET " + localVars.get(i) + " = " + 0 + "\n");
            lineNumber += 10;
        }
        for (int i = 0; i < localVars.size(); i++) {
            saveVars.append(lineNumber + " LET M(" + (i + 4) + ", f) = " + localVars.get(i) + "\n");
            lineNumber += 10;
        }

        return saveVars.toString();
    }

    private String saveCurrLocalVariables() {
        StringBuilder saveVars = new StringBuilder();

        //save parameters
        for (int i = 0; i < paramVars.size(); i++) {
            saveVars.append(lineNumber + " LET M(" + (i + 1) + ", f) = " + paramVars.get(i) + "\n");
            lineNumber += 10;
        }                       

        for (int i = 0; i < localVars.size(); i++) {
            saveVars.append(lineNumber + " LET M(" + (i + 4) + ", f) = " + localVars.get(i) + "\n");
            lineNumber += 10;
        }

        return saveVars.toString();
    }

    private String restoreLocalVariables() {
        StringBuilder restoreVars = new StringBuilder();

        for (int i = 0; i < paramVars.size(); i++) {
            restoreVars.append(lineNumber + " LET " + paramVars.get(i) + " = M(" + (i + 1) + ", f)\n");
            lineNumber += 10;
        }
        for (int i = 0; i < localVars.size(); i++) {
            restoreVars.append(lineNumber + " LET " + localVars.get(i) + " = M(" + (i + 4) + ", f)\n");
            lineNumber += 10;
        }

        return restoreVars.toString();
    }

    private String linkParameterVariables(SyntaxTree.Node functionBody) {
        StringBuilder saveVars = new StringBuilder();
        getParameterVariables(functionBody);

        for (int i = 0; i < paramVars.size(); i++) {
            saveVars.append(lineNumber + " LET " + paramVars.get(i) + " = M(" + (i + 1) + ", f)\n");
            lineNumber += 10;
        }

        return saveVars.toString();
    }

    private void getParameterVariables(SyntaxTree.Node functionBody) {
        if (functionBody != null && functionBody.value.equals("HEADER")) {
            for (SyntaxTree.Node child : functionBody.children) {
                if (child.value.equals("VNAME")) {
                    SymbolTable.VariableInfo varInfo = symbolTable.get(child.children.get(0).unid);
                    paramVars.add(varInfo.uniqueName);
                }
            }
        }
    }

}
