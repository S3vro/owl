package edu.kit.kastel.vads.compiler.parser.ast;

import java.util.Map;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IdentExpressionTree(NameTree name) implements ExpressionTree {
    @Override
    public Span span() {
        return name().span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    @Override
    public Type getType(Map<IdentName, Type> gamma) {
        return gamma.get(name.name());
    }
}
