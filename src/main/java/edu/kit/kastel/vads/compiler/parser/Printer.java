package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BitwiseNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.IntLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;

import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import java.util.List;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree(List<StatementTree> statements, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case FunctionTree(var returnType, var name, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("()");
                space();
                printTree(body);
            }
            case NameTree(var name, _) -> print(name.asString());
            case ProgramTree(var topLevelTrees) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case IntLiteralTree(var value, _, _) -> this.builder.append(value);
            case BoolLiteralTree(var value,_) -> this.builder.append(value);
            case NegateTree(var expression, _) -> {
                print("-(");
                printTree(expression);
                print(")");
            }
            case LogicalNegateTree(var expression, _) -> {
                print("!(");
                printTree(expression);
                print(")");
            }
            case AssignmentTree(var lValue, var op, var expression) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case IfTree(var expr, var then, var orElse, _) -> {
                print("if (");
                printTree(expr);
                print(")");
                space();
                printTree(then);
                if (orElse.isPresent()) {
                    print("else");
                    space();
                    printTree(orElse.get());
                }
                lineBreak();
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case WhileTree(var condition, var body, _) -> {
                print("while (");
                printTree(condition);
                print(")");
                space();
                printTree(body);
                lineBreak();
            }
            case TernaryTree(var condition, var trueBranch, var falseBranch) -> {
                printTree(condition);
                print(" ? ");
                printTree(trueBranch);
                print(" : ");
                printTree(falseBranch);
                lineBreak();

            }

            case BitwiseNegateTree(var expr, _) -> {
                print("~(");
                printTree(expr);
                print(")");
                lineBreak();
            }

            case ForTree(var def, var cond, var step, var body, _) -> {
                print("for");
                def.ifPresent(this::printTree);
                print(";");
                printTree(cond);
                print(";");
                step.ifPresent(this::printTree);
                print(")");
                space();
                printTree(body);
                lineBreak();
            }
            case BreakTree(_) -> {
                print("break");
                lineBreak();
            }
            case ContinueTree(_) -> {
                print("continue");
                lineBreak();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case IdentExpressionTree(var name) -> printTree(name);
        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
