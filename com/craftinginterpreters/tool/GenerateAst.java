/* 5.2.2 
 * We need to generate behavior-less classes 
 * that have a NAME and list of typed fields 
 * -> this script should automate the creation of boilerplate code for these classes
 * -> used for generating syntax trees 
 * 
 */


package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/* Primay usage: Java GenerateAst.java <output_dir> 
 * Automatically generates Expr.java
*/

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    // description of the classs we're creating 
    defineAst(outputDir, "Expr", Arrays.asList(
      "Assign   : Token name, Expr value",
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Logical  : Expr left, Token operator, Expr right",
      "Unary    : Token operator, Expr right",
      "Variable : Token name"
    ));
    defineAst(outputDir, "Stmt", Arrays.asList(
      "Block      : List<Stmt> statements",
      "Expression : Expr expression",
      "If         : Expr condition, Stmt thenBranch," +
                  " Stmt elseBranch",
      "Print      : Expr expression",
      "Var        : Token name, Expr initializer",
      "While      : Expr condition, Stmt body"
    ));
  }

  /* Function tod define a syntax tree and write specific items 
   * into a new file based on those new properties.
   */
  private static void defineAst(String outputDir, 
                                String baseName, 
                                List<String> types)
                                throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();

    // handle creation of abstract class to create blueprint for other classes
    writer.println("abstract class " + baseName + " {");
    defineVisitor(writer, baseName, types);


    // generate all the types 
    // trim() removes whitespace
    // for each type, split by a colon and remove whitespace, then use that data and define a type
    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim(); 
      defineType(writer, baseName, className, fields);
    }

    // write accept() method to call base methods defined in visitor template 
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");



    // end the file
    writer.println("}");
    writer.close();
  }

  /* Code to define a type, continues writing procedure  */
  private static void defineType(PrintWriter writer, 
                                 String baseName,
                                 String className, 
                                 String fieldList) {
    // more sugar to define header of class                                     
    writer.println("  static class " + className + " extends " +
        baseName + " {");

    // Constructor.
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store parameters in fields.
    // this should be belonging to each instance of each class 
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    // end the fields part in the constructor
    writer.println("    }");

    // Define fields as final member variables
    // by doing this in addition to the this-> instantiation earlier, 
    // we ensure AST is not changed after creation

    writer.println();
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }
    
    // put visitor pattern to implement accept() method from interface
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");

    // finally end our class
    writer.println("  }");

    
  }

  private static void defineVisitor(PrintWriter writer, 
                                    String baseName, 
                                    List<String> types) {

    // first define interface to say 'what the class is capable of'
    // contains signatures
    writer.println("  interface Visitor<R> {");

    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" +
          typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");

  }

  
}
