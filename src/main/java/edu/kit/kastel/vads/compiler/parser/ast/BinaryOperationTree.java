package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.lexer.Operator;

import java.util.Map;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import edu.kit.kastel.vads.compiler.semantic.TypeChecker;

public record BinaryOperationTree(
    ExpressionTree lhs, ExpressionTree rhs, Operator.OperatorType operatorType
) implements ExpressionTree {
    @Override
    public Span span() {
        return lhs().span().merge(rhs().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    @Override
    public Type getType(Map<IdentName, Type> gamma) {
        return TypeChecker.fromOperatorType(operatorType).res();
    }
}
