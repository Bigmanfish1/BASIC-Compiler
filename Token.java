public class Token {
    private String token;
    private static int idCounter = 1;
    private String tokenClass;
    private int id;

    public Token(String t, String c){
        this.token = t;
        this.tokenClass = c;
        this.id = Token.idCounter++;
    }

    public Token(int id,String t, String c){
        this.token = t;
        this.tokenClass = c;
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public String getTokenClass(){
        return this.tokenClass;
    }

    public String getToken(){
        return this.token;
    }

    @Override
    public String toString() {
        return "Token{id=" + this.id + " class='" + tokenClass + '\'' + ", token='" + token + '\'' + '}';
    }


}
