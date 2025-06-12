package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import java.util.List;

public class BreakAnalysis implements NoOpVisitor<List<StatementTree>> {

  @Override
  public Unit visit(BreakTree breakTree, List<StatementTree> data) {
    data.add(breakTree);
    return NoOpVisitor.super.visit(breakTree, data);
  }

  @Override
  public Unit visit(ContinueTree continueTree, List<StatementTree> data) {
    data.add(continueTree);
    return NoOpVisitor.super.visit(continueTree, data);
  }

  @Override
  public Unit visit(WhileTree blockTree, List<StatementTree> data) {
    data.clear();
    return NoOpVisitor.super.visit(blockTree, data);
  }

  @Override
  public Unit visit(ForTree forTree, List<StatementTree> data) {
    data.clear();
    return NoOpVisitor.super.visit(forTree, data);
  }

  @Override
  public Unit visit(FunctionTree functionTree, List<StatementTree> data) {
    if (!data.isEmpty()) {
      throw new SemanticException("break or continue outside of a loop");
    }
    return NoOpVisitor.super.visit(functionTree, data);
  }
}
