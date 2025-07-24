// This file is taken directly from Chapter 4 of the Crafting Interpreters Textbook ++ later parts
/* Class features:
 * Main error reporting functions
 * main() function & a starting point that ties the program together 
 * methods to call interpreter / compiling processes
 */
package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  // add bool to store if error occured
  static boolean hadError = false;
  static boolean hadRuntimeError = false;
  private static final Interpreter interpreter = new Interpreter();

  // main function
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  // function to take in a path, read all the bytes, convert it to a string, and
  // run it
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if (hadError) {
      System.exit(65); // exit program with code 65 if error'd
    }
    if (hadRuntimeError) 
    {
      System.exit(70);
    }
  }

  // function to run input items directly
  private static void runPrompt() throws IOException {

    // get the input via an inputstream and bufferedreader
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    // run file with code indefinitely until there are no more lines (hence null)
    for (;;) {

      // print > in a terminal-esque fashion
      System.out.print("> ");

      // prompt user for next line of input
      String line = reader.readLine();

      // end if the line is not valid
      if (line == null)
        break;

      // run each line
      run(line);
      hadError = false; // no error occurred here.

      /* Learning: code that reports error is separated from generation of error */
    }
  }

  // utilize Java's builtin scanner class to read each line
  // assume that source is the final path
  private static void run(String source) {
    // Scanner scanner = new Scanner(source);
    // List<Token> tokens = scanner.scanTokens();

    // // don't do anything with the tokens, but print.
    // for (Token token : tokens) {
    //   System.out.println(token);
    // }

    // comment out earlier code in favor for temporary testing 
    // see 6.4
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    Expr expression = parser.parse();

    // Stop if there was a syntax error.
    if (hadError) return;

    // this line simply prints out our AST
    // as of 7.4 we have a working intepreter, so  call that instead 
    //System.out.println(new AstPrinter().print(expression));

    interpreter.interpret(expression);

  }

  /* 4.1.1 Error handling */

  // why does this exist?
  /*
   * Users need to be able to check where their errors occur.
   * Users need to know what works and what fails internally
   * 
   * In our case, reporting exists in this class to satisfy some hadError bool
   * defined above.
   * 
   */

  // create function to display an error message at some int line #
  static void error(int line, String message) {
    report(line, "", message);
  }

  // create function to report the actual error (do the printing)
  private static void report(int line, String where, String message) {
    // print an error message using err builtin to java
    System.err.println("[line " + line + "] Error" + where + ": " + message);
  }

  /*
   * 4.2 Lexemes and Tokens
   * var language = "lox";
   * 
   * [var] [language] [=] ["lox"] [;]
   * is how we might parse this string
   * 
   * each [] is a lexeme
   */

  /* 6.3.2: More error handling */

  // given a token and a message, report a particular error message
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

}