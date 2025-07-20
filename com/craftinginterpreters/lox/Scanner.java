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
    /* c contains first char, current now points to the char after c,
     * match() checks the item at the current ptr
     */

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

      // notice the next three clauses 'do nothing',
      // the author originally included them to explicitly state that these are not defined operators
      // esp. because usually /r, ' ' or /t have meaning in other languages 
      // since we break, we just go forward and keep scanning. 
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      // \n is a standard newline char
      case '\n':
        line++;
        break;

      // 4.6.1 : if " then call the str func. 
      case '"': 
        string(); 
        break;


      // 4.6 account for the division operator 
      case '/':
        if (match('/')) {

          // while the next character is not a new line & we're not at the end of the string, keep going
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) { 
          // challenge: add support for c-style comments
          cStyleComment();
        } else {
          // otherwise, add the token!
          addToken(SLASH);
        }
        break;

      // 4.7 Add reserved words 
      case 'o':
        if (match('r')) {
          addToken(OR);
        }
        break;

      // add a special default case to error-check characters that don't meet the req. 
      default:
        // 4.6.1 add to error checker to account for digits
        if (isDigit(c)) 
        {
          number();
        } else if (isAlpha(c)) { /* 4.7 special case for if the item is an identifier, not an actual keyword */
          identifier();
        } else {
          Lox.error(line, "Unexpected chararater");
        }
        
        break;
    }
  }

  // advance the current pointer by 1, return the character stored at the prev location
  // current++ -> shorthand in java for incrementing a value in-place & returning value before incrementing
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

  // a function to see if we're at the end of our String
  private boolean isAtEnd() {
    return current >= source.length();
  }


  /* 4.6.1 */

  // once we start with a " ---> ignore everything that comes after it until we reach another "
  // then count that whole thing as one big TOKEN 
  private void string() {

    // continue if next char is not " and we're not at the end.
    while (peek() != '"' && !isAtEnd()) {

      // this clause enables multi-line strings
      if (peek() == '\n') {
        line++; 
      }

      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }


  /* 4.6.2 : Number literals */

  // support int, decimal, and float.

  // func that checks if char is within 0 <= c <= 9 
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  } 

  // method for parsing a number as one token 
  private void number() {

    // continue shifting ptr as long as the next item is a digit
    while (isDigit(peek())) {
      advance();
    }

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    // use built in Java parser 
    addToken(NUMBER,
        Double.parseDouble(source.substring(start, current)));
  }

  // peek TWO spaces ahead 
  private char peekNext() {

    // err check to see if we're out of bounds 
    if (current + 1 >= source.length()) {
      return '\0';
    }

    // return actual char
    return source.charAt(current + 1);
  } 

  /* 4.7 Reserved Words and Identifiers */
  // 
  /* CONCEPT: maximal munch
     Definiton: Whichever item created the longest length'd token is the correct item
     
     the provided example says that orchild would be preferred to or 
     because orchild is a longer token than or.     
     */

  // used to parse an identifier
  private void identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }

    // get the full text of the identifier
    String text = source.substring(start, current);

    // try to get the identifier in keywords
    TokenType type = keywords.get(text);

    // if not present, that means it is NOT RESERVED
    if (type == null) {
      type = IDENTIFIER;
    }

    // finally add the token
    addToken(type);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  // manually create hashmap to store all the other keywords required and their tokenType mapping
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("nil",    NIL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("this",   THIS);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
  }

  // challenge: c-style comment, based on string(), does not support nesting!
  private void cStyleComment() {

    // since we're at * when we start, increment ptr
    advance();

    // continue if next char is not * and  the one after is not '\\' and we're not at the end.
    while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {

      // this clause enables multi-line comments
      if (peek() == '\n') {
        line++; 
      }

      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    } 

    // The closing ".
    // unlike string, we MUST advance TWICE. The last clause already handles error checking here
    advance();
    advance();

    // Trim the surrounding quotes.
    // unlike string() we have to trim for TWO characters 
    String value = source.substring(start + 2, current - 2);

    // since the str ends with */ and we're currently at *, we need to keep going 1 space
    addToken(C_COMMENT, value);
  }

}
