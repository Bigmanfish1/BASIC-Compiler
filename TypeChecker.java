import java.util.Map;
public class TypeChecker {
    private Map<Integer, SymbolTable.VariableInfo> symbolTable;
    public TypeChecker(Map<Integer, SymbolTable.VariableInfo> symbolTable) {
        this.symbolTable = symbolTable;
    }   
    private String typeOf(SyntaxTree.Node node) throws Exception {

        // System.out.println("Node: " + node.value);

        if (node.value.equals("VTYP")) {
            if (node.children.get(0).value.equals("num")) {
                return "n";
            } else if (node.children.get(0).value.equals("text")) {
                return "t";
            } else {
                throw new Exception("Invalid type.");
            }
        } else if (node.value.equals("FTYP")) {
            if (node.children.get(0).value.equals("num")) {
                return "n";
            } else if (node.children.get(0).value.equals("void")) {
                return "v";
            } else {
                throw new Exception("Invalid type.");
            }

        } else if (node.value.equals("ATOMIC")) {
            return typeOf(node.children.get(0));
        } else if (node.value.equals("VNAME")) {
            int varUnid = node.children.get(0).unid;
            SymbolTable.VariableInfo variableInfo = symbolTable.get(varUnid);
            if (variableInfo == null) {
                throw new Exception("Variable '" + node.children.get(0).value + " ID: " + varUnid + "' not declared.");
            }
            // System.out.println("Variable '" + node.children.get(0).value + " ID: " + varUnid
            //         + "' is declared with type " + variableInfo.type);
            return variableInfo.type;
        } else if (node.value.equals("CONST")) {
            try {
                Integer.parseInt(node.children.get(0).value);
                return "n";
            } catch (NumberFormatException e) {
                return "t";
            }
        } else if (node.value.equals("FNAME")) {
            int funUnid = node.children.get(0).unid;
            SymbolTable.VariableInfo functionInfo = symbolTable.get(funUnid);
            if (functionInfo == null) {
                throw new Exception("Function '" + node.children.get(0).value + " ID: " + funUnid + "' not declared.");
            }
            // System.out.println("Function '" + node.children.get(0).value + " ID: " + funUnid
            //         + "' is declared with type " + functionInfo.type);
            return functionInfo.type;

        } else if (node.value.equals("TERM")) {
            return typeOf(node.children.get(0));
        } else if (node.value.equals("CALL")) {
            String t1 = typeOf(node.children.get(2));
            String t2 = typeOf(node.children.get(4));
            String t3 = typeOf(node.children.get(6));
            if (t1.equals(t2) && t2.equals(t3) && t1.equals("n")) {
                String t4 = typeOf(node.children.get(0));

                return t4;
            } else {
                return "u";
            }
        } else if (node.value.equals("UNOP")) {
            if (node.children.get(0).value.equals("sqrt")) {
                return "n";
            } else if (node.children.get(0).value.equals("not")) {
                return "b";
            } else {
                throw new Exception("Invalid type.");
            }

        } else if (node.value.equals("BINOP")) {
            switch (node.children.get(0).value) {
                case "add":
                    return "n";
                case "sub":
                    return "n";
                case "mul":
                    return "n";
                case "div":
                    return "n";
                case "eq":
                    return "c";
                case "grt":
                    return "c";
                case "or":
                    return "b";
                case "and":
                    return "b";
                default:
                    throw new Exception("Invalid type.");
            }

        } else if (node.value.equals("ARG")) {
            return typeOf(node.children.get(0));
        } else if (node.value.equals("OP")) {
            if (node.children.get(0).value.equals("UNOP")) {
                String t1 = typeOf(node.children.get(0));
                String t2 = typeOf(node.children.get(2));
                if (t1.equals(t2) && t1.equals("n")) {
                    return "n";
                } else if (t1.equals(t2) && t1.equals("b")) {
                    return "b";
                } else {
                    return "u";
                }
            } else if (node.children.get(0).value.equals("BINOP")) {
                String t0 = typeOf(node.children.get(0));
                String t1 = typeOf(node.children.get(2));
                String t2 = typeOf(node.children.get(4));
                if (t0.equals(t1) && t1.equals(t2) && t0.equals("n")) {
                    return "n";
                } else if (t0.equals(t1) && t1.equals(t2) && t0.equals("b")) {
                    return "b";
                } else if (t0.equals("c") && t1.equals(t2) && t1.equals("n")) {
                    return "b";
                } else {
                    return "u";
                }
            } else {
                return "u";
            }

        } else if (node.value.equals("COND")) {
            return typeOf(node.children.get(0));
        } else if (node.value.equals("SIMPLE")) {
            String t0 = typeOf(node.children.get(0));
            String t1 = typeOf(node.children.get(2));
            String t2 = typeOf(node.children.get(4));
            if (t0.equals(t1) && t1.equals(t2) && t0.equals("b")) {
                return "b";
            } else if (t0.equals("c") && t1.equals(t2) && t1.equals("n")) {
                return "b";
            } else {
                return "u";
            }

        } else if (node.value.equals("COMPOSIT")) {
            if (node.children.get(0).value.equals("BINOP")) {
                String t0 = typeOf(node.children.get(0));
                String t1 = typeOf(node.children.get(2));
                String t2 = typeOf(node.children.get(4));
                if (t0.equals(t1) && t1.equals(t2) && t0.equals("b")) {
                    return "b";
                } else {
                    return "u";
                }
            } else if (node.children.get(0).value.equals("UNOP")) {
                String t0 = typeOf(node.children.get(0));
                String t1 = typeOf(node.children.get(2));
                if (t0.equals(t1) && t0.equals("b")) {
                    return "b";
                } else {
                    return "u";
                }
            } else {
                return "u";
            }
        } else {
            throw new Exception("Invalid node type.");
        }
    }

    public boolean typeCheck(SyntaxTree.Node node) throws Exception {
        boolean result = true;
        switch (node.value) {
            case "PROG": {
                result = (typeCheck(node.children.get(1)) && typeCheck(node.children.get(2))
                        && typeCheck(node.children.get(3)));
                break;
            }
            case "GLOBVARS": {
                if (node.children.size() == 1) {
                    result = true;
                } else {
                    String type = typeOf(node.children.get(0));
                    SymbolTable.VariableInfo v = symbolTable.get(node.children.get(1).children.get(0).unid);
                    if (v == null) {
                        throw new Exception("Variable " + node.children.get(1).children.get(0).value + " ID: "
                                + node.children.get(1).children.get(0).unid + " not declared.");
                    }
                    v.type = type;
                    result = typeCheck(node.children.get(3));
                }
                break;
            }
            case "LOCVARS": {
                String t1 = typeOf(node.children.get(0));
                SymbolTable.VariableInfo v = symbolTable.get(node.children.get(1).children.get(0).unid);
                if (v == null) {
                    throw new Exception("Variable " + node.children.get(1).children.get(0).value + " ID: "
                            + node.children.get(1).children.get(0).unid + " not declared.");
                }
                v.type = t1;

                String t2 = typeOf(node.children.get(3));
                SymbolTable.VariableInfo v2 = symbolTable.get(node.children.get(4).children.get(0).unid);
                if (v2 == null) {
                    throw new Exception("Variable " + node.children.get(4).children.get(0).value + " ID: "
                            + node.children.get(4).children.get(0).unid + " not declared.");
                }
                v2.type = t2;

                String t3 = typeOf(node.children.get(6));
                SymbolTable.VariableInfo v3 = symbolTable.get(node.children.get(7).children.get(0).unid);
                if (v3 == null) {
                    throw new Exception("Variable " + node.children.get(7).children.get(0).value + " ID: "
                            + node.children.get(7).children.get(0).unid + " not declared.");
                }
                v3.type = t3;

                result = true;
                break;
            }
            case "COMMAND": {
                if (node.children.get(0).value.equals("skip")) {
                    result = true;
                } else if (node.children.get(0).value.equals("halt")) {
                    result = true;
                } else if (node.children.get(0).value.equals("print")) {
                    String t1 = typeOf(node.children.get(1));
                    if (t1.equals("n") || t1.equals("t")) {
                        result = true;
                    } else {
                        result = false;
                    }
                } else if (node.children.get(0).value.equals("return")) {
                    // tree-crawl to find function type
                    SyntaxTree.Node temp = node;
                    while (!temp.value.equals("FUNCTIONS")) {
                        temp = temp.parent;
                    }
                    String t1 = typeOf(node.children.get(1));

                    String t2 = typeOf(temp.children.get(0).children.get(0).children.get(0));
                    if (t1.equals(t2) && t1.equals("n")) {
                        result = true;
                    } else {
                        result = false;
                    }

                } else if (node.children.get(0).value.equals("ASSIGN")) {
                    result = typeCheck(node.children.get(0));
                } else if (node.children.get(0).value.equals("CALL")) {
                    String t1 = typeOf(node.children.get(0));
                    if (t1.equals("v")) {
                        result = true;
                    } else {
                        result = false;
                    }
                } else if (node.children.get(0).value.equals("BRANCH")) {
                    result = typeCheck(node.children.get(0));
                }
                break;
            }
            case "ALGO": {
                result = typeCheck(node.children.get(1));
                break;
            }
            case "INSTRUC": {
                if (node.children.size() == 1) {
                    result = true;
                } else {
                    result = (typeCheck(node.children.get(0)) && typeCheck(node.children.get(2)));
                }
                break;
            }
            case "BRANCH": {
                String t1 = typeOf(node.children.get(1));
                if (t1.equals("b")) {
                    result = typeCheck(node.children.get(3)) && typeCheck(node.children.get(5));
                } else {
                    result = false;
                }
                break;

            }
            case "ASSIGN": {
                if (node.children.size() == 2) {
                    String t1 = typeOf(node.children.get(0));
                    if (t1.equals("n")) {
                        result = true;
                    } else {
                        result = false;
                    }
                } else {
                    String t1 = typeOf(node.children.get(0));
                    String t2 = typeOf(node.children.get(2));
                    if (t1.equals(t2)) {
                        result = true;
                    } else {
                        result = false;
                    }
                }
                break;

            }
            case "FUNCTIONS": {
                if (node.children.size() == 1) {
                    result = true;
                } else {
                    result = (typeCheck(node.children.get(0)) && typeCheck(node.children.get(1)));
                }
                break;
            }
            case "DECL": {

                result = (typeCheck(node.children.get(0)) && typeCheck(node.children.get(1)));
                break;

            }
            case "HEADER": {
                String t1 = typeOf(node.children.get(0));
                SymbolTable.VariableInfo v = symbolTable.get(node.children.get(1).children.get(0).unid);
                if (v == null) {
                    throw new Exception("Variable " + node.children.get(1).children.get(0).value + " ID: "
                            + node.children.get(1).children.get(0).unid + " not declared.");
                }
                v.type = t1;
                String t2 = typeOf(node.children.get(3));
                String t3 = typeOf(node.children.get(5));
                String t4 = typeOf(node.children.get(7));

                if (t1.equals("n")) {
                    // tree-crawl to find return type
                    SyntaxTree.Node temp = node;
                    while (!temp.value.equals("DECL")) {
                        temp = temp.parent;
                    }
                    SyntaxTree.Node temp2 = temp.children.get(1).children.get(2).children.get(1);
                    while (temp2.children.size() != 1) {
                        if (temp2.children.get(0).children.get(0).value.equals("return")) {
                            break;
                        }
                        temp2 = temp2.children.get(2);
                    }
                    if (temp2.children.size() == 1)
                        result = false;
                    else {

                        if (t2.equals(t3) && t3.equals(t4) && t2.equals("n")) {
                            result = true;
                        } else {
                            result = false;
                        }
                    }
                } else {
                    if (t2.equals(t3) && t3.equals(t4) && t2.equals("n")) {
                        result = true;
                    } else {
                        result = false;
                    }
                }
                break;
            }
            case "BODY": {
                result = typeCheck(node.children.get(0)) && typeCheck(node.children.get(1))
                        && typeCheck(node.children.get(2)) && typeCheck(node.children.get(3))
                        && typeCheck(node.children.get(4));
                break;
            }
            case "SUBFUNCS": {
                result = typeCheck(node.children.get(0));
                break;
            }
            case "PROLOG": {
                result = true;
                break;
            }
            case "EPILOG": {
                result = true;
                break;
            }
            default:
                break;
        }

        for (SyntaxTree.Node child : node.children) {
            result = result && typeCheck(child);
        }


        return result;

    }

    public Map<Integer, SymbolTable.VariableInfo> getLargeSymbolTable() {
        return this.symbolTable;
    }

    public String printSymbolTable() {
        String res = "";
        if (symbolTable.isEmpty()) {
            res += "  No variables in this scope.\n";
            //System.out.println("  No variables in this scope.");
        } else {
            res += "----LARGE SYMBOL TABLE----\n";
            //System.out.println("----LARGE SYMBOL TABLE----");
            for (Map.Entry<Integer, SymbolTable.VariableInfo> entry : symbolTable.entrySet()) {
                res += "  Variable: " + entry.getKey() + " -> Unique Name: "
                        + entry.getValue().uniqueName + ", UNID: " + entry.getValue().unid + ", Original Name: "
                        + entry.getValue().originalName+ ", TYPE: " + entry.getValue().type + "\n";
              
            }
        }
        return res;
    }
    
}
