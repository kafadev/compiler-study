package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Variable;

/* Section 5.4
 * In 5.3 and earlier Expr.java is created as an interface and abstract class
 * to represent AST structures; this class follows the same paradigm and implements said
 * abstract class to create AstPrinter
 * 
 * Purpose: take an AST and print it into a readable lisp-like format
 *          (primarily for debugging)
 * 
 */

class AstPrinter implements Expr.Visitor<String> {

  // dummy main class to check whether the class functions accordingly 
  // (testing the debuggging class)
  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(
            new Expr.Literal(45.67)));

    System.out.println(new AstPrinter().print(expression));
  }

  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  /* Handle literal expressions via parenthesize()  
   * takes name & expr and wraps them via some string like (+ 1 2)
   * recursively calls itself depending on what the Expr is to formulate this structure
   * if there are NO EXPR, simply appends name and concludes
  */
  private String parenthesize(String name, Expr... exprs) {

    // StringBuilder offers alternative to String in Java.
    /* Exists to help for mutable string manipulation, i.e. append() etc. */
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  @Override
  public String visitAssignExpr(Assign expr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitAssignExpr'");
  }

  @Override
  public String visitVariableExpr(Variable expr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitVariableExpr'");
  }

}
