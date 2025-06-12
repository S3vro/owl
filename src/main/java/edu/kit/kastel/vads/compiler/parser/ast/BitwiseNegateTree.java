package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.IdentName;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Map;

public record BitwiseNegateTree(ExpressionTree expression, Span pos) implements ExpressionTree {
  @Override
  public Span span() {
    return pos().merge(expression().span());
  }

  @Override
  public <T, R> R accept(Visitor<T, R> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override
  public Type getType(Map<IdentName, Type> gamma) {
    return BasicType.INT;
  }
}
