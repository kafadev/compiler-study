package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();
  final Environment enclosing;

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {

    Object returnValue = null;
    
    if (values.containsKey(name.lexeme)) {
      returnValue = values.get(name.lexeme);
    }

    // what if it's not in the current scope? -> recurse.
    if (enclosing != null) {
        returnValue = enclosing.get(name);
    }

    if (returnValue != null) {
      return returnValue;
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

  // take advantage of Visitor paradigm again
  // cannot create a new variable! 
  void assign(Token name, Object value) {

    // if the item exists, then overwrite existing key
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    // if we can't assign it in the local scope, go up a level
    if (enclosing != null) {
        enclosing.assign(name, value);
        return;
    }

    /// otherwise, push error
    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

  void define(String name, Object value) {
    values.put(name, value);
  }


}
