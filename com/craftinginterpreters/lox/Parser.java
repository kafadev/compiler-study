package com.craftinginterpreters.lox;
import java.util.ArrayList;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/* class to parse through tokens and format them into proper ASTs 
 * 
 * Parser two jobs
 * 1. produce : valid tokens -> syntax tree 
 * 2. detect : invalid tokens -> error
 * -- 
 * for 2: 
 * a. must detect + report errors 
 * b. avoid crashing / hanging (exit gracefully)
 * 
 * other objectives
 * a. optimized implementation
 * b. classify, log and report errors 
 * c. do not report on errors caused by other errors
 * 
*/
public class Parser {
 
  // used so that we can return errors in a ParseError useful format
  private static class ParseError extends RuntimeException {}

  // define our list of tokens and which token we're currently on 
  private final List<Token> tokens;
  private int current = 0;

  // constructor to intialize our tokens
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  // try to parse() something, if it fails, return null after catching error
  // Expr parse() {
  //   try {
  //     return expression();
  //   } catch (ParseError error) {
  //     return null;
  //   }
  // }

  // new parsing function introduced in 8.1 that takes and returns statements
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(statement());
    }

    return statements; 
  }


  private Expr expression() {
    //return equality();
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }


  /* function to check if we're dealing with a binary expression, 
   * if so, then create a new expr and return it 
   * 
   * example:
   * a == b == c == d == e 
   * we evaluate each item as a new binary EXPR using the prev one as left op
   * 
   *    
   */
  private Expr equality() {
    Expr expr = comparison();

    // if inside this while loop, we know we're parsing an equality expr.  
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  /* function to check whether the current token (wherever our current ptr is in Tokens list) 
   * is equal to the enum TokenType provided
  */
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  /* The next four functions are supposed to emulate similar functionality to what's implemented in 
   * Scanner.java -> simplified ways to understand what value we're currently looking at & whether we're 
   * at the end of our list of tokens
   */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  /* various error checking items  */
  /* return a new parser object, sometimes the actual parser error 
   * does not require any overt synchronization,
   * in those cases, this func simply logs the error and returns a new ParseError obj.
   */
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /* In the case that an error occurs and we cannot restore our compilation flow,
   * use this function to synchronize our location in the code 
   * 
   * function operation:
     * Loop until we get to some semicolon,
     * or some reserved identifier
     * advance() to next item each time,
     * as a result, we can skip tokens until we get to something meaningful 
   */
  private void synchronize() {

    // go to the next token
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  /* comparison()
   * 
   * implement the following rule:
   * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * similar to equality() but with different types
   * calls term() instead of comparison() internally
   */
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
  
  /* check if we've got a MINUS/PLUS term
   * calls factor() in case we don't 
   */
  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /* function to handle any multiplication operation 
   * 
   */
  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /* Handle a unary operator
   * if we have ! or - handle as a unary op 
   * calls primary()  
   */ 
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  /* if it's not a unary expr, it must be a primary op
   * this func checks if it is a LITERAL or GROUPING
   */
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  /* 6.3 focuses on a sort of PANIC MODE
   * where the parser can detect an error and scrap later tokens
   * this is useful in reporting actually pertinent error messages
   */

  // checks if the next message is the expected type, otherwise throws an error 
  // if successful, runs & returns advance() 
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  /* 8.1 begins here */

  /* figure out what kind of statement it is, and execute accordingly.   */
  private Stmt statement() {
    if (match(PRINT)) return printStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }


  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  // handle a block
  /* 
   * 
   */
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    // ensure that the item is properly enclosed AND we're not at the end of our tokens
    // if so, add the statement
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    // get rid of } and error check
    consume(RIGHT_BRACE, "Expect '}' after block.");

    // return all our collected statements 
    return statements;
  }

  /* 8.4: in the case of variable assignment we don't want to evaluate LHS to token,
   * use this function to abstract away that problem and handle that case
   */
  private Expr assignment() {
    Expr expr = equality();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target."); 
    }

    return expr;
  }



}
