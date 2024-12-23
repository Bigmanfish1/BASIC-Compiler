import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Lexer {
    private String fileName;
    private static final Set<String> reserved_keywords = new HashSet<>(Arrays.asList(
            "main", "begin", "end", "skip", "halt", "print", "if", "then", "else",
            "num", "text", "void", "not", "sqrt", "or", "and", "eq", "grt", "add", "sub", "mul", "div",
            "< input", "=", "(", ")", ",", ";", "{", "}","return"));
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("V_[a-z][a-z0-9]*");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("F_[a-z][a-z0-9]*");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"[A-Z][a-z]{0,7}\"");
    private static final Pattern NUMBER_PATTERN = Pattern
            .compile("(0|([1-9][0-9]*))(\\.[0-9]+)?|-0(\\.[0-9]+)?|-[1-9][0-9]*(\\.[0-9]+)?");
    private List<Token> tokens;

    public Lexer(String fn) throws SyntaxException {
        this.fileName = fn;
        this.tokens = new ArrayList<>();
        readFile();

    }

    private void readFile() throws SyntaxException {
        try {
            Scanner sc = new Scanner(new File(this.fileName));
            
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                if (line.contains("<_input")) {
                    throw new SyntaxException("Invalid token '<_input' encountered in the source code.");
                }

                line = line.replace("< input", "<_input"); 
                
                String[] tokenList = line.split("\\s+");

                for (String token : tokenList) {
                    token = token.trim();  
                    
                    if (token.isEmpty()) continue; 

                    if (token.equals("<_input")) {
                        token = "< input";
                    }

                    Matcher matcher;

                    if (reserved_keywords.contains(token)) {
                        this.tokens.add(new Token(token, "reserved_keyword"));
                        continue;
                    }

                    matcher = VARIABLE_PATTERN.matcher(token);
                    if (matcher.matches()) {
                        this.tokens.add(new Token(token, "V_"));
                        continue;
                    }

                    matcher = FUNCTION_PATTERN.matcher(token);
                    if (matcher.matches()) {
                        this.tokens.add(new Token(token, "F_"));
                        continue;
                    }

                    matcher = STRING_PATTERN.matcher(token);
                    if (matcher.matches()) {
                        this.tokens.add(new Token(token, "T_"));
                        continue;
                    }

                    matcher = NUMBER_PATTERN.matcher(token);
                    if (matcher.matches()) {
                        this.tokens.add(new Token(token, "N_"));
                        continue;
                    }

                    throw new SyntaxException("Token does not belong to any class. Lexical Error! from token: " + token);
                }
            }

            sc.close();
        } catch (FileNotFoundException e) {
            System.out.println("File was not found.");
        }
    }

    public void createXML() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("TOKENSTREAM");
            doc.appendChild(rootElement);

            for (Token t : tokens) {
                Element tokElement = doc.createElement("TOK");
                rootElement.appendChild(tokElement);

                Element idElement = doc.createElement("ID");
                idElement.setTextContent(t.getId() + "");
                tokElement.appendChild(idElement);

                Element classElement = doc.createElement("CLASS");
                classElement.setTextContent(t.getTokenClass());
                tokElement.appendChild(classElement);

                Element wordElement = doc.createElement("WORD");
                wordElement.setTextContent(t.getToken());
                tokElement.appendChild(wordElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("tokens.xml"));
            transformer.transform(source, result);
            
            
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }

    }
}