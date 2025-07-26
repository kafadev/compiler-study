package com.craftinginterpreters.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object> ,
                                    Stmt.Visitor<Void> {

  private Environment environment = new Environment();

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
        checkNumberOperand(expr.operator, right); // checker to see if we can actually do our operation
        return -(double)right;
      case BANG:
        return !isTruthy(right);
    }

    // Unreachable.
    return null;
  }

  // evaluate a variable expressiopn
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  // error checker for if operator is put in a mistaken manner
  // triggers runtime error
  private void checkNumberOperand(Token operator, Object operand) {
    
    // if item is a double, all good
    if (operand instanceof Double) {
      return;
    }

    // otherwise, throw an error.
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  // another error checker
  private void checkNumberOperands(Token operator,
                                   Object left, 
                                   Object right) {
    // if the left and the right are doubles, we're all good.                                
    if (left instanceof Double && right instanceof Double) {
      return;
    }
    
    // if not, throw error!
    throw new RuntimeError(operator, "Operands must be numbers.");
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
  
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  // actually execute a block 
  void executeBlock(List<Stmt> statements,
                    Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  /* Visitor paradigm : set up for executing block statements */
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
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
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;

      // special case for PLUS to enable functionality of both 
      // i.e. "hello" + "world" AND 1 + 5
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
            return (double) left + (double) right;
        }
        else if (left instanceof String && right instanceof String) {
            return (String) left + (String) right;
        }

        // implement challenge 2 in chapter 7
        else if (left instanceof String && right instanceof Double) {
            return (String) left + right.toString();
        }
        else if (left instanceof Double && right instanceof String) {
            return left.toString() + (String) right;
        }

        // if the addition items are wrong, just toss an error
        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");

      // comparison cases
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
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

  /* Actually run the interpreter,
   * take in a syntax tree (Expr), then evaluate() it,
   * take that result and convert it to a string,
   * display the result
  */
  void interpret(List<Stmt> statements) { 
    // old approach
    // try {
    //   Object value = evaluate(expression);
    //   System.out.println(stringify(value));
    // } catch (RuntimeError error) {
    //   Lox.runtimeError(error);
    // }

    // chapter 8.1 approach
    // we need to be able to accept a list of Statements now
    try {
      for (Stmt statement : statements) {
        try {
          execute(statement);
        } catch (RuntimeError error) {

          // nifty conversion of statement -> expression
          if (statement instanceof Stmt.Expression) {
            Object value = evaluate(((Stmt.Expression) statement).expression);
            System.out.println(stringify(value));
          } else {
            Lox.runtimeError(error);
          }
          
        }
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  // handy method to converting an Object to string representation
  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }


  /* 7.4 Notes
   * 
   * Lox uses Double for all values (even ints)
   * 
   */

   /* 8.1: implement statements */
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  // just like print(), var is also a statement
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;

    // i.e. var x = 5;
    // var x;
    // statement 1. has an initializer
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }


  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }



}


