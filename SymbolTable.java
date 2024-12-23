import java.util.*;

public class SymbolTable {
    private Map<String, VariableInfo> symbolTable; // Use variable name as key and store additional info in VariableInfo
    private String scopeName; 
    private SymbolTable parentScope;
    private List<SymbolTable> childScopes;

    // Class to store variable information (name, UNID, etc.)
    public static class VariableInfo {
        public int unid;  // Unique ID from the syntax tree
        public String uniqueName; // Internally unique name generated for the variable
        public String type; // Type of the variable
        public String originalName;

        public VariableInfo(int unid, String uniqueName,String type, String originalName) {
            this.unid = unid;
            this.uniqueName = uniqueName;
            this.originalName = originalName;
            this.type = type;
        }
        public VariableInfo(int unid, String uniqueName, String originalName) {
            this.unid = unid;
            this.uniqueName = uniqueName;
            this.originalName = originalName;
            this.type = "";
        }
    }

    public SymbolTable(String scopeName, SymbolTable parentScope) {
        this.symbolTable = new HashMap<>();
        this.scopeName = scopeName;
        this.parentScope = parentScope;
        this.childScopes = new ArrayList<>();
    }

    public void put(String varName, int unid, String uniqueName,String type) {
        symbolTable.put(varName, new VariableInfo(unid, uniqueName,type, varName)); // Store the UNID and unique name
    }

    public void put(String varName, int unid, String uniqueName) {
        symbolTable.put(varName, new VariableInfo(unid, uniqueName, varName)); // Store the UNID and unique name
    }

    public Map<String, VariableInfo> getSymbolTable(){
        return this.symbolTable;
    }

    public boolean contains(String varName) {
        return symbolTable.containsKey(varName); // Check if variable name exists
    }

    public VariableInfo get(String varName) {
        return symbolTable.get(varName); // Get the VariableInfo by variable name
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public SymbolTable getParentScope() {
        return parentScope;
    }

    public void addChildScope(SymbolTable childScope) {
        childScopes.add(childScope); // Add the child scope to the list
    }

    public List<SymbolTable> getChildScopes() {
        return childScopes; // Return the list of child scopes
    }

    // Modified to print the variables with indentation
    public void printSymbolTable(String indent) {
        if (symbolTable.isEmpty()) {
            System.out.println(indent + "  No variables in this scope.");
        } else {
            for (Map.Entry<String, VariableInfo> entry : symbolTable.entrySet()) {
                System.out.println(indent + "  Variable: " + entry.getKey() + " -> Unique Name: " 
                                    + entry.getValue().uniqueName + ", UNID: " + entry.getValue().unid + ", Original Name: " + entry.getValue().originalName);
            }
        }
    }

    public void printSymbolTable() {
        System.out.println("\n--- Scope: " + scopeName + " ---");
        if (symbolTable.isEmpty()) {
            System.out.println("No entries in this scope.");
        } else {
            for (Map.Entry<String, VariableInfo> entry : symbolTable.entrySet()) {
                System.out.println("Variable: " + entry.getKey() + " -> Unique Name: " + entry.getValue().uniqueName + ", UNID: " + entry.getValue().unid + ", Original Name: " + entry.getValue().originalName);
            }
        }
        System.out.println("----------------------");
    }
}
