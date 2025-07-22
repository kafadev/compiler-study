package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {

  /* Create function to visit Literal and get the value directly */
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  /* Evaluate unary expressions, 
   * unary is defined as -> having one single subexpression that req. eval first
   * in this case, we don't know what expr.right is, so evaluate() that
   * since a UNARY, we know LEFT is - or ! which means we must negate -> use Java built-in for that
  */
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        return -(double)right;
      case BANG:
        return !isTruthy(right);
    }

    // Unreachable.
    return null;
  }

  /* Visit a grouping expression [parenthesis], run evaluate() to simplify further */
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  /* Reuse Visitor pattern for simplified evaluation of diff. kinds of expressions */
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  /*  */
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {

    // evaluate left & right
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); 

    // handle what to do based on the type
    switch (expr.operator.type) {
      case MINUS:
        return (double)left - (double)right;
      case SLASH:
        return (double)left / (double)right;
      case STAR:
        return (double)left * (double)right;

      // special case for PLUS to enable functionality of both 
      // i.e. "hello" + "world" AND 1 + 5
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
            return (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
            return (String) left + (String) right;
        }

      // comparison cases
      case GREATER:
        return (double)left > (double)right;
      case GREATER_EQUAL:
        return (double)left >= (double)right;
      case LESS:
        return (double)left < (double)right;
      case LESS_EQUAL:
        return (double)left <= (double)right;

      // brute equality
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
    }

    // Unreachable.
    return null;
  }

  /* Ruleset for what is a truthy value
   * Mimicks Ruby's implementation
   * false, nil -> falsey
   * all else -> truthey
   * 
   * if statements put in succession -> if one finishes, other two not checked
   * third clause ensures that 0 is falsey -> commented out to follow book
   */
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    // if (object instanceof Number) {
    //     double value = ((Number) object).doubleValue();
    //     return (value != 0);        // if not 0, return true
    // }
    return true;
  }

  /* Host equality in another function 
   * Lox Equality emulates Java equality
  */
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }







}


