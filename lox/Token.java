package com.craftinginterpreters.lox;

/* This class handles the Token structure 
 * ------
 * this contains a lexeme and all the surrounding data of *what* that actually means
 * Since we have no idea what this item is, we store is as an Object type (base class in Java)
 * TokenType is self explanatory, int line refers to the line it is on 
 */ 
class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line; 

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}