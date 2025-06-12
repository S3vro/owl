package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.*;

public class RecursivePreOrderVisitor<T, R> implements Visitor<T, R> {
    private final Visitor<T, R> visitor;

    public RecursivePreOrderVisitor(Visitor<T, R> visitor) {
        this.visitor = visitor;
    }

    @Override
    public R visit(AssignmentTree assignmentTree, T data) {
        R r = this.visitor.visit(assignmentTree, data);
        r = assignmentTree.lValue().accept(this, accumulate(data, r));
        r = assignmentTree.expression().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BinaryOperationTree binaryOperationTree, T data) {
        R r = this.visitor.visit(binaryOperationTree, data);
        r = binaryOperationTree.lhs().accept(this, accumulate(data, r));
        r = binaryOperationTree.rhs().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        R r = this.visitor.visit(blockTree, data);
        T d = accumulate(data, r);
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = accumulate(d, r);
        }
        return r;
    }

    @Override
    public R visit(DeclarationTree declarationTree, T data) {
        R r = this.visitor.visit(declarationTree, data);
        r = declarationTree.type().accept(this, accumulate(data, r));
        r = declarationTree.name().accept(this, accumulate(data, r));
        if (declarationTree.initializer() != null) {
            r = declarationTree.initializer().accept(this, accumulate(data, r));
        }
        return r;
    }

    @Override
    public R visit(FunctionTree functionTree, T data) {
        R r = this.visitor.visit(functionTree, data);
        r = functionTree.returnType().accept(this, accumulate(data, r));
        r = functionTree.name().accept(this, accumulate(data, r));
        r = functionTree.body().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IdentExpressionTree identExpressionTree, T data) {
        R r = this.visitor.visit(identExpressionTree, data);
        r = identExpressionTree.name().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IntLiteralTree literalTree, T data) {
        return this.visitor.visit(literalTree, data);
    }

    @Override
    public R visit(LValueIdentTree lValueIdentTree, T data) {
        R r = this.visitor.visit(lValueIdentTree, data);
        r = lValueIdentTree.name().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NameTree nameTree, T data) {
        return this.visitor.visit(nameTree, data);
    }

    @Override
    public R visit(NegateTree negateTree, T data) {
        R r = this.visitor.visit(negateTree, data);
        r = negateTree.expression().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(LogicalNegateTree negateTree, T data) {
        R r = this.visitor.visit(negateTree, data);
        r = negateTree.expression().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BitwiseNegateTree bitwiseNegateTree, T data) {
        R r = this.visitor.visit(bitwiseNegateTree, data);
        r = bitwiseNegateTree.expression().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ProgramTree programTree, T data) {
        R r = this.visitor.visit(programTree, data);
        T d = accumulate(data, r);
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = tree.accept(this, d);
            d = accumulate(d, r);
        }
        return r;
    }

    @Override
    public R visit(ReturnTree returnTree, T data) {
        R r = this.visitor.visit(returnTree, data);
        r = returnTree.expression().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TypeTree typeTree, T data) {
        return this.visitor.visit(typeTree, data);
    }

    protected T accumulate(T data, R value) {
        return data;
    }

    @Override
    public R visit(BoolLiteralTree literalTree, T data) {
        return this.visitor.visit(literalTree, data);
    }

    @Override
    public R visit(IfTree ifTree, T data) {
        R r = this.visitor.visit(ifTree, data);
        r = ifTree.e().accept(this, accumulate(data, r));
        r = ifTree.then().accept(this, accumulate(data, r));
        if (ifTree.orElse().isPresent()) {
            r = ifTree.orElse().get().accept(this, accumulate(data, r));
        }
        return r;
    }

    @Override
    public R visit(WhileTree whileTree, T data) {
        R r = this.visitor.visit(whileTree, data);
        r = whileTree.condition().accept(this, accumulate(data, r));
        r = whileTree.body().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TernaryTree ternaryTree, T data) {
        R r = this.visitor.visit(ternaryTree, data);
        r = ternaryTree.condition().accept(this, accumulate(data, r));
        r = ternaryTree.trueBranch().accept(this, accumulate(data, r));
        r = ternaryTree.falseBranch().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ForTree forTree, T data) {
        R r = this.visitor.visit(forTree, data);
        if (forTree.definition().isPresent()) {
            r = forTree.definition().get().accept(this, accumulate(data, r));
            r = forTree.condition().accept(this, accumulate(data, r));
        } else {
            r = forTree.condition().accept(this, accumulate(data, r));
        }

        if (forTree.statement().isPresent()) {
            r = forTree.statement().get().accept(this, accumulate(data, r));
        }

        r = forTree.body().accept(this, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ContinueTree continueTree, T data) {
        return this.visitor.visit(continueTree, data);
    }

    @Override
    public R visit(BreakTree breakTree, T data) {
        return this.visitor.visit(breakTree, data);
    }
}
