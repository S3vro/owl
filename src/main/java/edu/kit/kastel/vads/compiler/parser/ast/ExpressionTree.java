package edu.kit.kastel.vads.compiler.parser.ast;

import java.util.Map;

import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.Type;

public sealed interface ExpressionTree extends Tree
  permits BinaryOperationTree, BoolLiteralTree, IdentExpressionTree, IntLiteralTree, LogicalNegateTree, NegateTree {
    Type getType(Map<IdentName, Type> gamma);
}
