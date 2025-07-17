/* Started in 4.4 */ 

// note this class revises the regular Scanner implementation in Java
// for one that has tokenizer functionality

package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// static import allows for unqualified access to static members without inheriting from type
// use if we don't want to re-declare local constants
// generally not good practice -> used for shorthand
import static com.craftinginterpreters.lox.TokenType.*; 

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();

  // needed for our token scanner
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }

  // scan through source code, adding tokens until we run out of chars 
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      
      // we're at the beginning of the next lexemme
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  /* 4.5 starts here */

  // scanToken() looks at whatever the current character in question is and then checks what it is
  // we take advantage of the ENUM in TokenType to define what's going on
  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break; 

      /* sometimes it's the case that equality requires two characters, 
          i.e. !=, ==, >=, etc
          to error check this, use a custom func match  to see if the current char is the same
      */ 

      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;

      // add a special default case to error-check characters that don't meet the req. 
      default:
        Lox.error(line, "Unexpected chararater");
        break;
    }
  }

  // advance the current pointer by 1, return the character stored at the advanced location
  // current++ -> shorthand in java for incrementing a value in-place & returning incremented value
  private char advance() {
    return source.charAt(current++);
  }

  // wrapper around more complex add token function
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  // actual method for creating new token 
  // adds token to list of all tokens
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  /* 
    custom function to check if the expected value is equal or not to the current value
    used to see if the next char is something related for equality cases
  */
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  /* 4.6 special handling for / characters */
  /* the peek function should check the next char without advancing current
     since we use the lookahead to scan for later items, one char. at a time is the most efficient
     1,2+ chars makes it less efficient since we repeat over data more often (?)
   */
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

}
