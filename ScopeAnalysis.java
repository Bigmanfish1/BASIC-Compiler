import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class ScopeAnalysis {
    private int uniqueVariableIdentifierCounter;
    private int uniqueFunctionIdentifierCounter;
    private int uniqueAlgoIdentifierCounter;
    private SymbolTable currentScope;
    private List<String> reservedKeywords;
    private List<UnresolvedFunctionCall> unresolvedCalls = new ArrayList<>();
    private Map<Integer, SymbolTable.VariableInfo> symbolTable = new HashMap<>();

    public ScopeAnalysis() {
        uniqueFunctionIdentifierCounter = 1;
        uniqueVariableIdentifierCounter = 1;
        uniqueAlgoIdentifierCounter = 1;
        currentScope = null;
        reservedKeywords = Arrays.asList(
                "main", "begin", "end", "skip", "halt", "print", "if", "then", "else",
                "num", "text", "void", "not", "sqrt", "or", "and", "eq", "grt", "add", "sub", "mul", "div",
                "< input", "=", "(", ")", ",", ";", "{", "}", "return");
    }

    private static class UnresolvedFunctionCall {
        String functionName;
        SyntaxTree.Node callNode;
        SymbolTable callScope;

        public UnresolvedFunctionCall(String functionName, SyntaxTree.Node callNode, SymbolTable callScope) {
            this.functionName = functionName;
            this.callNode = callNode;
            this.callScope = callScope;
        }
    }

    public SyntaxTree parseXMLToSyntaxTree(String filePath) {
        SyntaxTree syntaxTree = new SyntaxTree();
        Map<Integer, SyntaxTree.Node> nodeMap = new HashMap<>();
        NodeList innerNodeList;
        NodeList leafNodeList;

        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            doc.getDocumentElement().normalize();

            Element rootElement = (Element) doc.getElementsByTagName("ROOT").item(0);
            int rootUnid = Integer.parseInt(rootElement.getElementsByTagName("UNID").item(0).getTextContent());
            String rootSymb = rootElement.getElementsByTagName("SYMB").item(0).getTextContent();
            syntaxTree.createRoot(rootSymb, rootUnid);
            nodeMap.put(rootUnid, syntaxTree.root);

            // Parse and store inner nodes in nodeMap
            innerNodeList = doc.getElementsByTagName("IN");
            for (int i = 0; i < innerNodeList.getLength(); i++) {
                Element inNodeElement = (Element) innerNodeList.item(i);
                int unid = Integer.parseInt(inNodeElement.getElementsByTagName("UNID").item(0).getTextContent());
                String symb = inNodeElement.getElementsByTagName("SYMB").item(0).getTextContent();
                SyntaxTree.Node innerNode = syntaxTree.createInnerNode(symb, null, unid);
                nodeMap.put(unid, innerNode);
            }

            // Parse and store leaf nodes in nodeMap
            leafNodeList = doc.getElementsByTagName("LEAF");
            for (int i = 0; i < leafNodeList.getLength(); i++) {
                Element leafElement = (Element) leafNodeList.item(i);
                int unid = Integer.parseInt(leafElement.getElementsByTagName("UNID").item(0).getTextContent());
                String terminal = leafElement.getElementsByTagName("TERMINAL").item(0).getTextContent();
                SyntaxTree.Node leafNode = syntaxTree.createLeafNode(terminal, null, unid);
                nodeMap.put(unid, leafNode);
            }

            // Build the tree recursively starting from the root node
            buildTreeRecursively(syntaxTree.root, rootElement, innerNodeList, leafNodeList, nodeMap);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return syntaxTree;
    }

    private void buildTreeRecursively(SyntaxTree.Node currentNode, Element currentElement, NodeList innerNodeList,
            NodeList leafNodeList, Map<Integer, SyntaxTree.Node> nodeMap) {
        NodeList children = currentElement.getElementsByTagName("CHILDREN").item(0).getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals("ID")) {
                int childUnid = Integer.parseInt(children.item(i).getTextContent());
                SyntaxTree.Node childNode = nodeMap.get(childUnid);

                if (childNode != null) {
                    currentNode.children.add(childNode);
                    childNode.parent = currentNode;

                    // If the child is an inner node, recursively process its children
                    for (int j = 0; j < innerNodeList.getLength(); j++) {
                        Element innerElement = (Element) innerNodeList.item(j);
                        int innerUnid = Integer
                                .parseInt(innerElement.getElementsByTagName("UNID").item(0).getTextContent());

                        if (innerUnid == childUnid) {
                            buildTreeRecursively(childNode, innerElement, innerNodeList, leafNodeList, nodeMap);
                        }
                    }
                }
            }
        }
    }

    public void analyze(SyntaxTree.Node root) throws Exception {
        if (root == null) {
            throw new Exception("Invalid syntax tree.");
        }
        traverseAndAnalyze(root);

        resolveFunctionCalls();

        printScopeTree();

        consolidateSymbolTables();

        printSymbolTable();// print large symbol table

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

        if (node.value.equals("end") || node.value.equals("PROG")) {
            exitCurrentScope();
        }
        // System.out.println(node.value + " " + result + " Afterwards");

        return result;

    }

    private void traverseAndAnalyze(SyntaxTree.Node node) throws Exception {
        switch (node.value) {
            case "PROG":
                enterNewScope("main");
                break;
            case "FUNCTIONS":
                checkFunctionDeclaration(node);
                break;
            case "GLOBVARS":
            case "LOCVARS":
                checkVariableDeclaration(node);
                break;
            case "COMMAND":
                checkCommand(node);
                break;
            case "COND":
                checkCond(node);
                break;
            case "ALGO":
                enterNewScope("algo" + uniqueAlgoIdentifierCounter++);
                break;
            default:
                break;
        }

        for (SyntaxTree.Node child : node.children) {
            traverseAndAnalyze(child);
        }

        if (node.value.equals("end") || node.value.equals("PROG")) {
            exitCurrentScope();
        }
    }

    private void enterNewScope(String scopeName) {
        SymbolTable newScope = new SymbolTable(scopeName, currentScope);
        if (currentScope != null) {
            currentScope.addChildScope(newScope);
        }
        currentScope = newScope;
    }

    private void exitCurrentScope() {
        if (currentScope != null && currentScope.getParentScope() != null) {
            currentScope = currentScope.getParentScope();
        }
    }

    private SymbolTable getCurrentScope() {
        return currentScope;
    }

    private String generateUniqueVarName() {
        return "varName" + uniqueVariableIdentifierCounter++;
    }

    private String generateUniqueFuncName() {
        return "functionName" + uniqueFunctionIdentifierCounter++;
    }

    private void checkVariableDeclaration(SyntaxTree.Node node) throws Exception {
        SymbolTable currentScope = getCurrentScope();

        if (node.value.equals("GLOBVARS")) {
            processGlobalVars(node, currentScope);
        }

        else if (node.value.equals("LOCVARS")) {
            processLocalVars(node, currentScope);
        }
    }

    private void processGlobalVars(SyntaxTree.Node node, SymbolTable currentScope) throws Exception {
        for (int i = 0; i < node.children.size(); i++) {
            SyntaxTree.Node child = node.children.get(i);

            if (child.value.equals("VTYP")) {
                SyntaxTree.Node vnameNode = node.children.get(++i);

                if (!vnameNode.value.equals("VNAME")) {
                    throw new Exception("Expected a variable name after type declaration.");
                }

                SyntaxTree.Node varNode = vnameNode.children.get(0);
                String varName = varNode.value;
                int varUnid = varNode.unid;

                if (currentScope.contains(varName)) {
                    throw new Exception("Variable '" + varName + "' already declared in this scope.");
                }
                if (reservedKeywords.contains(varName)) {
                    throw new Exception("Variable name '" + varName + "' cannot be a reserved keyword.");
                }

                String uniqueName = generateUniqueVarName();
                currentScope.put(varName, varUnid, uniqueName);
            } else if (child.value.equals(",")) {
                continue;
            }
        }
    }

    private void processLocalVars(SyntaxTree.Node node, SymbolTable currentScope) throws Exception {
        if (node.children.size() != 9) {
            throw new Exception("LOCVARS must declare exactly 3 variables.");
        }

        for (int i = 0; i < node.children.size(); i++) {
            SyntaxTree.Node child = node.children.get(i);

            if (child.value.equals("VTYP")) {
                SyntaxTree.Node vnameNode = node.children.get(++i);

                if (!vnameNode.value.equals("VNAME")) {
                    throw new Exception("Expected a variable name after type declaration.");
                }

                SyntaxTree.Node varNode = vnameNode.children.get(0);
                String varName = varNode.value;
                int varUnid = varNode.unid;

                if (currentScope.contains(varName)) {
                    throw new Exception("Variable '" + varName + "' already declared in this scope.");
                }
                if (reservedKeywords.contains(varName)) {
                    throw new Exception("Variable name '" + varName + "' cannot be a reserved keyword.");
                }

                String uniqueName = generateUniqueVarName();
                currentScope.put(varName, varUnid, uniqueName);
            } else if (child.value.equals(",")) {
                continue;
            }
        }
    }

    private void checkVariableAssignment(SyntaxTree.Node node) throws Exception {
        if (node.children.isEmpty()) {
            throw new Exception("ASSIGN node has no children.");
        }

        SyntaxTree.Node vnameNode = node.children.get(0);
        if (!vnameNode.value.equals("VNAME")) {
            throw new Exception("Expected VNAME in ASSIGN node.");
        }

        SyntaxTree.Node variableNameNode = vnameNode.children.get(0);
        String varName = variableNameNode.value;

        SymbolTable.VariableInfo variableInfo = lookupVariable(varName);

        if (variableInfo == null) {
            throw new Exception("Variable '" + varName + "' not declared.");
        }

        variableNameNode.unid = variableInfo.unid;
        // System.out.println("Variable '" + varName + "' is declared with UNID " + variableInfo.unid);

        if (node.children.size() == 2 && node.children.get(1).value.equals("< input")) {
            return;
        } else if (node.children.size() == 3 && node.children.get(1).value.equals("=")) {
            SyntaxTree.Node termNode = node.children.get(2);
            checkTerm(termNode);
        } else {
            throw new Exception("Invalid ASSIGN structure.");
        }
    }

    private void checkTerm(SyntaxTree.Node termNode) throws Exception {
        // The node passed here is TERM, now check its child to determine the type
        if (termNode.children.isEmpty()) {
            throw new Exception("TERM node has no children.");
        }

        SyntaxTree.Node childNode = termNode.children.get(0); // This can be ATOMIC, CALL, or OP

        if (childNode.value.equals("ATOMIC")) {
            checkAtomic(childNode); // Handle ATOMIC
        } else if (childNode.value.equals("CALL")) {
            // Kaybee added this
            checkFunctionCall(childNode);
        } else if (childNode.value.equals("OP")) {
            checkOperation(childNode); // Handle operations
        } else {
            throw new Exception("Invalid TERM type.");
        }
    }

    private void checkAtomic(SyntaxTree.Node atomicNode) throws Exception {
        if (atomicNode.children.isEmpty()) {
            throw new Exception("ATOMIC node has no children.");
        }

        SyntaxTree.Node atomicChild = atomicNode.children.get(0); // Could be VNAME or CONST

        if (atomicChild.value.equals("VNAME")) {
            SyntaxTree.Node variableNode = atomicChild.children.get(0); // Actual variable node
            String varName = variableNode.value; // Use the variable's name

            SymbolTable.VariableInfo variableInfo = lookupVariable(varName);

            if (variableInfo == null) {
                throw new Exception("Variable '" + varName + "' not declared.");
            }
            variableNode.unid = variableInfo.unid;
        } else if (atomicChild.value.equals("CONST")) {
            // Constants don't need lookup but can be handled for type checking
        } else {
            throw new Exception("Invalid ATOMIC value.");
        }
    }

    private void checkOperation(SyntaxTree.Node opNode) throws Exception {
        SyntaxTree.Node opTypeNode = opNode.children.get(0);

        if (opTypeNode.value.equals("UNOP")) {
            SyntaxTree.Node argNode = opNode.children.get(2);
            checkArg(argNode);
        } else if (opTypeNode.value.equals("BINOP")) {
            SyntaxTree.Node firstArgNode = opNode.children.get(2);
            SyntaxTree.Node secondArgNode = opNode.children.get(4);
            checkArg(firstArgNode);
            checkArg(secondArgNode);
        } else {
            throw new Exception("Invalid operation type.");
        }
    }

    private void checkArg(SyntaxTree.Node argNode) throws Exception {
        if (argNode.children.isEmpty()) {
            throw new Exception("ARG node has no children.");
        }

        // Check if the argument is an ATOMIC or an OP
        SyntaxTree.Node childNode = argNode.children.get(0); // Get the first child of ARG

        if (childNode.value.equals("ATOMIC")) {
            // Now check the child of ATOMIC
            SyntaxTree.Node atomicChild = childNode.children.get(0);
            if (atomicChild.value.equals("VNAME")) {
                // Handle variable name case
                String varName = atomicChild.children.get(0).value; // Get the variable name

                SymbolTable.VariableInfo variableInfo = lookupVariable(varName);
                if (variableInfo == null) {
                    throw new Exception("Variable '" + varName + "' not declared.");
                }
                atomicChild.children.get(0).unid = variableInfo.unid;
            } else if (atomicChild.value.equals("CONST")) {
                // CONST node represents a constant value, no further validation needed here
                // You might want to add any necessary checks for the constant value here if
                // needed
            } else {
                throw new Exception("Invalid ATOMIC value in ARG.");
            }
        } else if (childNode.value.equals("OP")) {
            // If the child is an OP, delegate to checkOperation
            checkOperation(childNode);
        } else {
            throw new Exception("Invalid ARG type: expected ATOMIC or OP.");
        }
    }

    private void checkFunctionDeclaration(SyntaxTree.Node node) throws Exception {
        if (node.children.isEmpty()) {
            throw new Exception("FUNCTIONS node has no children.");
        }

        if (node.children.get(0).value.equals("e")) {
            return;
        }

        SyntaxTree.Node declNode = node.children.get(0);
        SyntaxTree.Node headerNode = null;
        SyntaxTree.Node bodyNode = null;

        for (SyntaxTree.Node child : declNode.children) {
            if (child.value.equals("HEADER")) {
                headerNode = child;
            } else if (child.value.equals("BODY")) {
                bodyNode = child;
            }
        }

        if (headerNode == null || bodyNode == null) {
            throw new Exception("Malformed function declaration. HEADER or BODY missing.");
        }

        SyntaxTree.Node fnameNode = headerNode.children.get(1);

        String functionName = fnameNode.children.get(0).value;

        if (reservedKeywords.contains(functionName)) {
            throw new Exception("Function name '" + functionName + "' cannot be a reserved keyword.");
        }

        SymbolTable currentScope = getCurrentScope();

        if (currentScope.contains(functionName)) {
            throw new Exception(
                    "Function '" + functionName + "': sibling already declared with the same name in this scope.");
        }

        if (currentScope.getScopeName().equals(functionName)) {
            throw new Exception("Function '" + functionName + "': child scope has same name as parent scope.");
        }

        String uniqueFuncName = generateUniqueFuncName();
        String t;
        // Kaybee added function type to symbol table
        try {
            t = typeOf(headerNode.children.get(0));
            currentScope.put(functionName, fnameNode.children.get(0).unid, uniqueFuncName, t);
        } catch (Exception e) {
            currentScope.put(functionName, fnameNode.children.get(0).unid, uniqueFuncName);
            ;
        }

        enterNewScope(functionName);

        currentScope = getCurrentScope();

        for (int i = 2; i < headerNode.children.size(); i++) {
            SyntaxTree.Node paramNode = headerNode.children.get(i);
            if (paramNode.value.equals("VNAME")) {
                SyntaxTree.Node paramNameNode = paramNode.children.get(0);
                String paramName = paramNameNode.value;

                if (reservedKeywords.contains(paramName)) {
                    throw new Exception("Parameter name '" + paramName + "' cannot be a reserved keyword.");
                }
                if (currentScope.contains(paramName)) {
                    throw new Exception("Parameter '" + paramName + "' already declared in function scope.");
                }
                // Kaybee added default type for parameters
                currentScope.put(paramName, paramNameNode.unid, generateUniqueVarName(), "n");
            }
        }
    }

    private void resolveFunctionCalls() throws Exception {
        for (UnresolvedFunctionCall call : unresolvedCalls) {

            if (isInMainScope(call.callScope) && call.functionName.equals("main")) {
                throw new Exception("Recursive calls to 'main' are not allowed.");
            }

            String uniqueName = lookupFunctionInScopeOrParentScopes(call.functionName, call.callScope);
            SymbolTable.VariableInfo result = lookupFunction(call.functionName, call.callScope);
            if(result == null){
                throw new Exception("Function call to '" + call.functionName + "' cannot be resolved.");
            }
            call.callNode.children.get(0).children.get(0).unid = result.unid;
            if (uniqueName == null) {
                throw new Exception("Function call to '" + call.functionName + "' cannot be resolved.");
            } else {
                System.out.println("Function call to '" + call.functionName
                        + "' successfully resolved with unique name " + uniqueName);
            }
        }
    }

    private void checkFunctionCall(SyntaxTree.Node node) throws Exception {
        if (node.children.isEmpty()) {
            throw new Exception("CALL node has no children.");
        }

        // Retrieve function name
        SyntaxTree.Node functionNameNode = node.children.get(0);
        String functionName = functionNameNode.children.get(0).value;

        for (int i = 2; i < node.children.size(); i += 2) {
            SyntaxTree.Node atomicNode = node.children.get(i);
            validateAtomicNode(atomicNode);
        }

        unresolvedCalls.add(new UnresolvedFunctionCall(functionName, node, currentScope));
    }

    private void validateAtomicNode(SyntaxTree.Node atomicNode) throws Exception {
        if (!atomicNode.value.equals("ATOMIC")) {
            throw new Exception("Expected ATOMIC node in function call.");
        }

        SyntaxTree.Node childNode = atomicNode.children.get(0);

        if (childNode.value.equals("VNAME")) {
            SyntaxTree.Node variableNode = childNode.children.get(0);
            String varName = variableNode.value;

            SymbolTable.VariableInfo variableInfo = lookupVariable(varName); // Look up variable in scope
            if (variableInfo == null) {
                throw new Exception("Variable '" + varName + "' used in function call has not been declared.");
            }
            variableNode.unid = variableInfo.unid;
            System.out.println("Argument '" + varName + "' in function call is declared at UNID " + variableInfo.unid);
        } else if (childNode.value.equals("CONST")) {
            System.out.println("Constant argument in function call is valid.");
        } else {
            throw new Exception("Invalid argument in function call: expected VNAME or CONST.");
        }
    }

    private void checkCommand(SyntaxTree.Node node) throws Exception {
        if (node.children.isEmpty()) {
            throw new Exception("COMMAND node has no children.");
        }

        SyntaxTree.Node commandNode = node.children.get(0);

        switch (commandNode.value) {
            case "print":
                checkPrint(node.children.get(1)); // Check print ATOMIC
                break;
            case "return":
                checkReturn(node.children.get(1)); // Check return ATOMIC
                break;
            case "ASSIGN":
                checkVariableAssignment(node.children.get(0)); // Handle assignment
                break;
            case "CALL":
                checkFunctionCall(node.children.get(0)); // Handle function calls
                break;
        }
    }

    private void checkCond(SyntaxTree.Node node) throws Exception {
        if (node.children.isEmpty()) {
            throw new Exception("COND node has no children.");
        }

        SyntaxTree.Node condChild = node.children.get(0);

        if (condChild.value.equals("SIMPLE")) {
            checkSimple(condChild);
        } else if (condChild.value.equals("COMPOSIT")) {
            checkComposit(condChild);
        } else {
            throw new Exception("Invalid COND child: expected SIMPLE or COMPOSIT.");
        }
    }

    private void checkSimple(SyntaxTree.Node simpleNode) throws Exception {
        if (simpleNode.children.size() != 6) {
            throw new Exception("Invalid SIMPLE structure: expected BINOP( ATOMIC , ATOMIC ).");
        }

        SyntaxTree.Node binopNode = simpleNode.children.get(0);
        if (!binopNode.value.equals("BINOP")) {
            throw new Exception("Expected BINOP in SIMPLE.");
        }

        checkAtomic(simpleNode.children.get(2));
        checkAtomic(simpleNode.children.get(4));
    }

    private void checkComposit(SyntaxTree.Node compositNode) throws Exception {
        if (compositNode.children.size() == 6) {
            SyntaxTree.Node binopNode = compositNode.children.get(0);
            if (!binopNode.value.equals("BINOP")) {
                throw new Exception("Expected BINOP in COMPOSIT.");
            }

            checkSimple(compositNode.children.get(2));
            checkSimple(compositNode.children.get(4));
        } else if (compositNode.children.size() == 4) {
            SyntaxTree.Node unopNode = compositNode.children.get(0);
            if (!unopNode.value.equals("UNOP")) {
                throw new Exception("Expected UNOP in COMPOSIT.");
            }

            checkSimple(compositNode.children.get(2));
        } else {
            throw new Exception("Invalid COMPOSIT structure.");
        }
    }

    private void checkPrint(SyntaxTree.Node atomicNode) throws Exception {
        checkAtomic(atomicNode);
    }

    private void checkReturn(SyntaxTree.Node atomicNode) throws Exception {
        SymbolTable current = currentScope;
        boolean inFunctionScope = false;

        while (current != null) {
            if (current.getScopeName().startsWith("F_")) {
                inFunctionScope = true;
                break;
            }

            if (current.getScopeName().equals("main")) {
                throw new Exception("'return' statement cannot appear in the 'main' function.");
            }

            current = current.getParentScope();
        }

        if (!inFunctionScope) {
            throw new Exception("'return' statement must appear within a function scope.");
        }
        checkAtomic(atomicNode);
    }

    private boolean isInMainScope(SymbolTable scope) {
        while (scope != null) {
            if (scope.getScopeName().equals("main")) {
                return true;
            }
            scope = scope.getParentScope();
        }
        return false;
    }

    private boolean isRecursiveScope(SymbolTable scope, String fName) {
        while (scope != null) {
            if (scope.getScopeName().equals(fName)) {
                return true;
            }
            scope = scope.getParentScope();
        }
        return false;
    }

    private String lookupFunctionInScopeOrParentScopes(String funcName, SymbolTable startScope) {// Ask if function
                                                                                                 // calls can call from
                                                                                                 // parent
        SymbolTable scope = startScope;

        // For recursive calls
        if (scope != null && isRecursiveScope(scope, funcName)) {
            System.out.println("Recursive call to " + funcName);
            while (scope != null) {
                if (scope.contains(funcName)) {
                    return scope.get(funcName).uniqueName;
                }
                scope = scope.getParentScope();
            }
        }

        while (scope != null) {
            if (scope.contains(funcName)) {
                return scope.get(funcName).uniqueName;
            }
            scope = scope.getParentScope();
        }
        return null;
    }

    private SymbolTable.VariableInfo lookupFunction(String funcName, SymbolTable startScope) {// Ask if function
        // calls can call from
        // parent
        SymbolTable scope = startScope;

        // For recursive calls
        if (scope != null && isRecursiveScope(scope, funcName)) {
            System.out.println("Recursive call to " + funcName);
            while (scope != null) {
                if (scope.contains(funcName)) {
                    return scope.get(funcName);
                }
                scope = scope.getParentScope();
            }
        }

        while (scope != null) {
            if (scope.contains(funcName)) {
                return scope.get(funcName);
            }
            scope = scope.getParentScope();
        }
        return null;
    }

    private SymbolTable.VariableInfo lookupVariable(String varName) {
        SymbolTable scope = currentScope;
        while (scope != null) {
            if (scope.contains(varName)) {
                return scope.get(varName);
            }
            scope = scope.getParentScope();
        }
        return null;
    }

    private SymbolTable getHeadScope() {
        SymbolTable head = currentScope;
        while (head != null && head.getParentScope() != null) {
            head = head.getParentScope();
        }
        return head;
    }

    private void printScopeTree(SymbolTable scope, int depth) {
        if (scope == null)
            return;

        String indent = "  ".repeat(depth);
        System.out.println(indent + "Scope: " + scope.getScopeName());

        scope.printSymbolTable(indent);

        for (SymbolTable child : scope.getChildScopes()) {
            printScopeTree(child, depth + 1);
        }
    }

    public void printScopeTree() {
        SymbolTable head = getHeadScope();
        printScopeTree(head, 0);
    }

    public void consolidateSymbolTables() {
        SymbolTable rootScope = getHeadScope();
        collectSymbolsFromScope(rootScope, symbolTable);
    }

    private void collectSymbolsFromScope(SymbolTable scope, Map<Integer, SymbolTable.VariableInfo> consolidatedTable) {
        for (Map.Entry<String, SymbolTable.VariableInfo> entry : scope.getSymbolTable().entrySet()) {
            consolidatedTable.put(entry.getValue().unid, entry.getValue());
        }

        for (SymbolTable childScope : scope.getChildScopes()) {
            collectSymbolsFromScope(childScope, consolidatedTable);
        }
    }

    public void printSymbolTable() {
        if (symbolTable.isEmpty()) {
            System.out.println("  No variables in this scope.");
        } else {
            System.out.println("----LARGE SYMBOL TABLE----");
            for (Map.Entry<Integer, SymbolTable.VariableInfo> entry : symbolTable.entrySet()) {
                System.out.println("  Variable: " + entry.getKey() + " -> Unique Name: "
                        + entry.getValue().uniqueName + ", UNID: " + entry.getValue().unid + ", Original Name: "
                        + entry.getValue().originalName+ ", TYPE: " + entry.getValue().type);
            }
        }
    }

    public Map<Integer, SymbolTable.VariableInfo> getLargeSymbolTable() {
        return this.symbolTable;
    }

}
