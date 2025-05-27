package edu.kit.kastel.vads.compiler.parser.ast;

import java.util.Optional;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public final record IfTree(ExpressionTree e, StatementTree then, Optional<StatementTree> orElse, Position start) implements StatementTree{

    @Override
    public Span span() {
        Span ifSpan = e.span().merge(then.span());

        Span end = orElse.isPresent() ? ifSpan.merge(orElse.get().span()) : ifSpan;

        return new Span.SimpleSpan(start, end.end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
    
}
