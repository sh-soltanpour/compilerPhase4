import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;

// Visit https://stackoverflow.com/questions/26451636/how-do-i-use-antlr-generated-parser-and-lexer
public class Atalk {
  public static void main(String[] args) throws IOException {
    ANTLRInputStream reader = new ANTLRFileStream(args[0]);

    // Pass #1
    AtalkPass1Lexer pass1Lexer = new AtalkPass1Lexer(reader);
    CommonTokenStream pass1Tokens = new CommonTokenStream(pass1Lexer);
    AtalkPass1Parser pass1Parser = new AtalkPass1Parser(pass1Tokens);
    ParseTree pass1Tree = pass1Parser.program(); // Program is starting production rule
    // System.out.println(pass1Tree.toStringTree());

    // Pass #2
    if (Tools.codeIsValid){
      reader.reset();
      AtalkPass2Lexer pass2Lexer = new AtalkPass2Lexer(reader);
      CommonTokenStream pass2Tokens = new CommonTokenStream(pass2Lexer);
      AtalkPass2Parser pass2Parser = new AtalkPass2Parser(pass2Tokens);
      ParseTree pass2Tree = pass2Parser.program();
    }
  }
}