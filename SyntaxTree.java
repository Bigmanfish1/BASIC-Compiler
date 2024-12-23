import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SyntaxTree {
    public Node root;
    private int currentUNID = 0; // Counter for generating unique node IDs

    // Node class definition
    public static class Node {
        String value; // This can be a terminal or non-terminal symbol
        Node parent;
        List<Node> children = new ArrayList<>();
        int unid; // Unique Node ID

        public Node(String value, int unid) {
            this.value = value;
            this.unid = unid;
        }
    }

    // Method to create the root of the tree
    public void createRoot(String value) {
        this.root = new Node(value, currentUNID++);
    }

    // Method to create leaf nodes
    public Node createLeafNode(String value, Node parent) {
        Node newLeaf = new Node(value, currentUNID++);
        if (parent != null) {
            parent.children.add(newLeaf);
            newLeaf.parent = parent;
        }
        return newLeaf;
    }

    // Method to create inner nodes
    public Node createInnerNode(String value, Node parent) {
        Node newNode = new Node(value, currentUNID++);
        if (parent != null) {
            parent.children.add(newNode);
            newNode.parent = parent;
        }
        return newNode;
    }

    public void createRoot(String value, int unid) {
        this.root = new Node(value, unid);
    }

    // Method to create a leaf node with a specific UNID
    public Node createLeafNode(String value, Node parent, int unid) {
        Node newLeaf = new Node(value, unid);
        if (parent != null) {
            parent.children.add(newLeaf);
            newLeaf.parent = parent;
        }
        return newLeaf;
    }

    // Method to create an inner node with a specific UNID
    public Node createInnerNode(String value, Node parent, int unid) {
        Node newNode = new Node(value, unid);
        if (parent != null) {
            parent.children.add(newNode);
            newNode.parent = parent;
        }
        return newNode;
    }

    // Method to save the syntax tree to an XML file
    public void saveToXML(String filePath) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("SYNTREE");
        doc.appendChild(rootElement);

        // Create ROOT element
        Element rootNodeElement = doc.createElement("ROOT");
        rootElement.appendChild(rootNodeElement);
        
        // Add UNID for the root
        rootNodeElement.appendChild(createElementWithText(doc, "UNID", String.valueOf(root.unid)));
        
        // Add SYMB for the root
        rootNodeElement.appendChild(createElementWithText(doc, "SYMB", root.value));
        
        // Add CHILDREN element for the root
        Element childrenElement = doc.createElement("CHILDREN");
        for (Node child : root.children) {
            childrenElement.appendChild(createElementWithText(doc, "ID", String.valueOf(child.unid)));
        }
        rootNodeElement.appendChild(childrenElement);

        // Add INNERNODES element
        Element innerNodesElement = doc.createElement("INNERNODES");
        rootElement.appendChild(innerNodesElement);
        addInnerNodesToXML(doc, innerNodesElement, root);

        // Add LEAFNODES element
        Element leafNodesElement = doc.createElement("LEAFNODES");
        rootElement.appendChild(leafNodesElement);
        addLeafNodesToXML(doc, leafNodesElement, root);

        // Create transformer and write to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));

        transformer.transform(source, result);
    }

    private void addInnerNodesToXML(Document doc, Element parentElement, Node node) {
        for (Node child : node.children) {
            if (child.children.size() > 0) { // Only process inner nodes
                Element innerNodeElement = doc.createElement("IN");
                
                // PARENT
                innerNodeElement.appendChild(createElementWithText(doc, "PARENT", String.valueOf(node.unid)));
                // UNID
                innerNodeElement.appendChild(createElementWithText(doc, "UNID", String.valueOf(child.unid)));
                // SYMB
                innerNodeElement.appendChild(createElementWithText(doc, "SYMB", child.value));
                
                // CHILDREN
                Element childrenElement = doc.createElement("CHILDREN");
                for (Node grandchild : child.children) {
                    childrenElement.appendChild(createElementWithText(doc, "ID", String.valueOf(grandchild.unid)));
                }
                innerNodeElement.appendChild(childrenElement);

                parentElement.appendChild(innerNodeElement);
                
                // Recursively add inner nodes
                addInnerNodesToXML(doc, parentElement, child);
            }
        }
    }

    private void addLeafNodesToXML(Document doc, Element parentElement, Node node) {
        for (Node child : node.children) {
            if (child.children.isEmpty()) { // Only process leaf nodes
                Element leafElement = doc.createElement("LEAF");
                
                // PARENT
                leafElement.appendChild(createElementWithText(doc, "PARENT", String.valueOf(node.unid)));
                // UNID
                leafElement.appendChild(createElementWithText(doc, "UNID", String.valueOf(child.unid)));
                // TERMINAL
                Element terminalElement = doc.createElement("TERMINAL");
                terminalElement.setTextContent(child.value); // Assuming child.value contains the token directly
                leafElement.appendChild(terminalElement);
                
                parentElement.appendChild(leafElement);
            } else {
                addLeafNodesToXML(doc, parentElement, child); // Recurse for inner nodes
            }
        }
    }

    private Element createElementWithText(Document doc, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.setTextContent(textContent);
        return element;
    }

    public Node getRoot() {
        return root;
    }

    public void printTree(Node node, String indent, boolean isTail) {
        System.out.println(indent + (isTail ? "└── " : "├── ") + "Node[UNID=" + node.unid + ", Value=" + node.value + "]");
        for (int i = 0; i < node.children.size() - 1; i++) {
            printTree(node.children.get(i), indent + (isTail ? "    " : "│   "), false);
        }
        if (node.children.size() > 0) {
            printTree(node.children.get(node.children.size() - 1), indent + (isTail ? "    " : "│   "), true);
        }
    }

    // Method to print the entire tree starting from the root
    public void printSyntaxTree() {
        if (root != null) {
            printTree(root, "", true);
        }
    }
}
