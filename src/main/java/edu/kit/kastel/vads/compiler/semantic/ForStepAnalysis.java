package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class ForStepAnalysis implements NoOpVisitor<Boolean> {
  @Override
  public Unit visit(ForTree forTree, Boolean data) {
    if (forTree.statement().isPresent()) {
      if (forTree.statement().get() instanceof DeclarationTree) {
        throw new SemanticException("the step statement in a for loop cannot be a declaration");
      }
    }

    return Unit.INSTANCE;
  }
}
