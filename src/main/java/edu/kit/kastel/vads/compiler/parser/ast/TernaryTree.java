package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Map;

public record TernaryTree(ExpressionTree condition, ExpressionTree trueBranch, ExpressionTree falseBranch) implements ExpressionTree {

  @Override
  public Span span() {
    return condition.span().merge(trueBranch.span().merge(falseBranch.span()));
  }

  @Override
  public Type getType(Map<IdentName, Type> gamma) {
    return trueBranch.getType(gamma);
  }

  @Override
  public <T, R> R accept(Visitor<T, R> visitor, T data) {
    return visitor.visit(this, data);
  }
}
