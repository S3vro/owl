package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePreOrderVisitor;
import java.util.ArrayList;
import java.util.HashMap;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new BreakAnalysis()), new ArrayList<>());
        this.program.accept(new RecursivePostorderVisitor<>(new ForStepAnalysis()), true);
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());
        this.program.accept(new VariableStatusAnalysis(), new VariableStatusAnalysis.VariableStatus(new ArrayList<>()));
        this.program.accept(new RecursivePreOrderVisitor<>(new TypeChecker()), new HashMap<>());
    }

}
