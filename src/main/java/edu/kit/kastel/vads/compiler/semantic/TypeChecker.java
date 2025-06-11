package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import java.util.Map;

import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class TypeChecker implements NoOpVisitor<Map<IdentName, Type>>{


    @Override
    public Unit visit(DeclarationTree declarationTree, Map<IdentName, Type> data) {
        if (declarationTree.initializer() != null) {
            if (!declarationTree.initializer().getType(data).equals(declarationTree.type().type())) {
                throw new SemanticException("type checking for declaration " + declarationTree + " failed due to wrong expression type");
            }
        }

        if (declarationTree.name().name() instanceof IdentName name) {
            data.put(name, declarationTree.type().type());
        }

        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(IfTree literalTree, Map<IdentName, Type> data) {
        boolean valid = literalTree.e().getType(data).equals(BasicType.BOOL);
            if (!valid) throw new SemanticException("expression"+ literalTree.e() + " at " + literalTree.e().span() + " should be of type bool");
        return NoOpVisitor.super.visit(literalTree, data);
    }

  @Override
  public Unit visit(ReturnTree returnTree, Map<IdentName, Type> data) {
      if (returnTree.expression().getType(data).equals(BasicType.BOOL)) {
        throw new SemanticException("this compiler can currently only return ints!");
      }
    return NoOpVisitor.super.visit(returnTree, data);
  }

  @Override
  public Unit visit(WhileTree tree, Map<IdentName, Type> data) {
    boolean valid = tree.condition().getType(data).equals(BasicType.BOOL);
    if (!valid) throw new SemanticException("expression"+ tree.condition() + " at " + tree.condition().span() + " should be of type bool");
    return NoOpVisitor.super.visit(tree, data);
  }

  @Override
  public Unit visit(TernaryTree ternaryTree, Map<IdentName, Type> data) {
      if (ternaryTree.trueBranch().getType(data) != ternaryTree.falseBranch().getType(data)) {
        throw new SemanticException("both branches of a ternary must have the same type!");
      }
    return NoOpVisitor.super.visit(ternaryTree, data);
  }

  @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, Map<IdentName, Type> data) {
        OperatorType op = binaryOperationTree.operatorType();
        if (op == OperatorType.LOGICAL_EQUAL || op == OperatorType.LOGICAL_UNEQUAL) {
            // handly polymorphic types
            if (binaryOperationTree.lhs().getType(data).equals(binaryOperationTree.rhs().getType(data))) {
                return NoOpVisitor.super.visit(binaryOperationTree, data);
            }

            throw new SemanticException(String.format("type checking for %s failed", binaryOperationTree));
        }

        BinaryOperatorType binaryOperatorType = fromOperatorType(op);
        Type lhs = binaryOperatorType.lhs();
        Type rhs = binaryOperatorType.rhs();
        Type res = binaryOperatorType.res();

        boolean valid = lhs.equals(binaryOperationTree.lhs().getType(data))
                    && rhs.equals(binaryOperationTree.rhs().getType(data))
                    && res.equals(binaryOperationTree.getType(data));

        if (!valid)
            throw new SemanticException(String.format("type checking for %s failed", binaryOperationTree));

        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }

    @Override
    public Unit visit(AssignmentTree assignmentTree, Map<IdentName, Type> data) {
        if (assignmentTree.lValue() instanceof LValueIdentTree idTree) {
            boolean valid = assignmentTree.expression().getType(data).equals(data.get(idTree.name().name()));
            if (valid)
                return NoOpVisitor.super.visit(assignmentTree, data);
        }
        throw new SemanticException(String.format("type checking for %s failed", assignmentTree));
    }


    public static BinaryOperatorType fromOperatorType(OperatorType type) {
            return switch(type) {
                case MUL,
                     DIV,
                     PLUS,
                     MINUS,
                     MOD,
                     RSHIFT,
                     LSHIFT,
                     BITWISE_AND,
                     BITWISE_OR,
                     BITWISE_XOR -> new BinaryOperatorType(BasicType.INT, BasicType.INT, BasicType.INT);
                case LOGICAL_GT,
                     LOGICAL_GT_OR_EQUAL,
                     LOGICAL_LT,
                     LOGICAL_LT_OR_EQUAL
                     -> new BinaryOperatorType(BasicType.BOOL, BasicType.INT, BasicType.INT);
                case LOGICAL_AND,
                     LOGICAL_OR
                     -> new BinaryOperatorType(BasicType.BOOL, BasicType.BOOL, BasicType.BOOL);
                case LOGICAL_EQUAL,
                     LOGICAL_UNEQUAL
                     -> new BinaryOperatorType(BasicType.BOOL, BasicType.BOOL, BasicType.BOOL);
                default -> throw new IllegalArgumentException(type + " is not a binary operator");
            };
    }

    public static record BinaryOperatorType(Type res, Type lhs, Type rhs){

    }
}
