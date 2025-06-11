package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Optional;

public record ForTree(Optional<StatementTree> definition,  ExpressionTree condition, Optional<StatementTree> statement, StatementTree body, Position start) implements StatementTree {

    @Override
    public Span span() {
        Span span = condition.span().merge(body.span());

        return new Span.SimpleSpan(start, span.end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
