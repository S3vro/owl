package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import java.util.ArrayList;
import java.util.List;

class ReturnAnalysis implements NoOpVisitor<ReturnAnalysis.ReturnState> {

    static class ReturnState {
        List<StatementTree> statements = new ArrayList<>();
    }

    @Override
    public Unit visit(ReturnTree returnTree, ReturnState data) {
        data.statements.add(returnTree);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, ReturnState data) {
        List<StatementTree> returns = blockTree.statements().stream()
          .filter(s -> data.statements.contains(s))
          .toList();

        if (!returns.isEmpty()) {
            data.statements.add(blockTree);
        }

        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, ReturnState data) {
        if (data.statements.contains(ifTree.then())) {
            if (ifTree.orElse().isPresent() && data.statements.contains(ifTree.orElse().get())) {
                data.statements.add(ifTree);
            }
        }
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, ReturnState data) {
        if (!data.statements.contains(functionTree.body())) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        data.statements.clear();
        return NoOpVisitor.super.visit(functionTree, data);
    }
}
