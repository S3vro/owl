package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.optimize.RemoveDeadBlocks;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BitwiseNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.IntLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
    private final FunctionTree function;
    private final GraphConstructor constructor;

    public SsaTranslation(FunctionTree function, List<Optimizer> optimizer) {
        this.function = function;
        this.constructor = new GraphConstructor(optimizer, function.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.function.accept(visitor, this);
        new RemoveDeadBlocks().remove(this.constructor.graph());
        return this.constructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.constructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.constructor.readVariable(variable, block);
    }

    private Block currentBlock() {
        return this.constructor.currentBlock();
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.getDebugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.constructor::newSub;
                case ASSIGN_PLUS -> data.constructor::newAdd;
                case ASSIGN_MUL -> data.constructor::newMul;
                case ASSIGN_XOR -> data.constructor::newXor;
                case ASSIGN_AND -> data.constructor::newBitWiseAnd;
                case ASSIGN_OR -> data.constructor::newBitwiseOr;
                case ASSIGN_LSHIFT ->  data.constructor::newLShiftNode;
                case ASSIGN_RSHIFT ->  data.constructor::newRShiftNode;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case ASSIGN -> null;
                default ->
                    throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        private Optional<Node> calculate_stepwise( BinaryOperationTree tree, SsaTranslation data) {
            pushSpan(tree);
            Node lhs = tree.lhs().accept(this, data).orElseThrow();

            Block nextStepBlock = new Block(data.constructor.graph(), "stepwise_calculation_next_" + Math.abs(tree.hashCode()));
            Block afterCalculation = new Block(data.constructor.graph(), "stepwise_calculation_after_" + Math.abs(tree.hashCode()));

            boolean isLogicalAnd = tree.operatorType() == Operator.OperatorType.LOGICAL_AND;
            Block caseTrue = isLogicalAnd ? nextStepBlock : afterCalculation;
            Block caseFalse = isLogicalAnd ? afterCalculation : nextStepBlock;

            Node conditionalNode = data.constructor.newIfNode(lhs, caseTrue, caseFalse);
            Node trueProj = data.constructor.newControlFlowProj(conditionalNode, ProjNode.SimpleProjectionInfo.CF_1);
            Node falseProj = data.constructor.newControlFlowProj(conditionalNode, ProjNode.SimpleProjectionInfo.CF_0);
            nextStepBlock.addPredecessor(isLogicalAnd ? trueProj : falseProj);
            data.constructor.sealBlock(nextStepBlock);
            afterCalculation.addPredecessor(isLogicalAnd ? falseProj : trueProj);

            data.constructor.switchBlock(nextStepBlock);
            Node rhs = tree.rhs().accept(this, data).orElseThrow();
            Node exitJmp = data.constructor.newJmp(afterCalculation);
            afterCalculation.addPredecessor(exitJmp);
            data.constructor.sealBlock(afterCalculation);

            data.constructor.switchBlock(afterCalculation);
            Phi phi = data.constructor.newPhi();
            phi.appendOperand(lhs);
            phi.appendOperand(rhs);

            popSpan();

            return Optional.of(data.constructor.tryRemoveTrivialPhi(phi));
        }

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {

            if (binaryOperationTree.operatorType() == Operator.OperatorType.LOGICAL_AND || binaryOperationTree.operatorType() == Operator.OperatorType.LOGICAL_OR) {
                return calculate_stepwise(binaryOperationTree, data);
            }

            pushSpan(binaryOperationTree);
            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MINUS -> data.constructor.newSub(lhs, rhs);
                case PLUS -> data.constructor.newAdd(lhs, rhs);
                case MUL -> data.constructor.newMul(lhs, rhs);
                case BITWISE_XOR -> data.constructor.newXor(lhs, rhs);
                case BITWISE_AND -> data.constructor.newBitWiseAnd(lhs, rhs);
                case LOGICAL_AND -> data.constructor.newLogicalAnd(lhs, rhs);
                case LOGICAL_EQUAL -> data.constructor.newLogicalEqual(lhs, rhs);
                case LOGICAL_UNEQUAL -> data.constructor.newLogicalUnequal(lhs, rhs);
                case LOGICAL_OR -> data.constructor.newLogicalOr(lhs, rhs);
                case BITWISE_OR -> data.constructor.newBitwiseOr(lhs, rhs);
                case LOGICAL_LT -> data.constructor.newLessThanNode(lhs, rhs);
                case LOGICAL_LT_OR_EQUAL -> data.constructor.newLessThanOrEqualNode(lhs, rhs);
                case LSHIFT -> data.constructor.newLShiftNode(lhs, rhs);
                case LOGICAL_GT -> data.constructor.newGreaterThanNode(lhs, rhs);
                case LOGICAL_GT_OR_EQUAL -> data.constructor.newGreaterThanOrEqualNode(lhs, rhs);
                case RSHIFT -> data.constructor.newRShiftNode(lhs, rhs);
                case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                default ->
                    throw new IllegalArgumentException("not a binary expression operator " + binaryOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);
            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                // skip everything after a return in a block
                if (statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            if (declarationTree.initializer() != null) {
                Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);
            Node start = data.constructor.newStart();
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(IntLiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(BoolLiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstBool(literalTree.value());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newSub(data.constructor.newConstInt(0), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.constructor.newReturn(node);
            data.constructor.graph().endBlock().addPredecessor(ret);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.constructor.newSideEffectProj(divMod);
            data.constructor.writeCurrentSideEffect(projSideEffect);
            return data.constructor.newResultProj(divMod);
        }

        @Override
        public Optional<Node> visit(LogicalNegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newXor(data.constructor.newConstInt(1), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);
            Block thenBlock = new Block(data.constructor.graph(), "if_true_" + Math.abs(ifTree.hashCode()));
            Block elseBlock = new Block(data.constructor.graph(), "if_false_"+ Math.abs(ifTree.hashCode()));
            Block afterIf = new Block(data.constructor.graph(), "after_if_" +  Math.abs(ifTree.hashCode()));

            Node exp = ifTree.e().accept(this, data).orElseThrow();
            Node ifNode = data.constructor.newIfNode(exp, thenBlock, ifTree.orElse().isPresent() ? elseBlock : afterIf);
            data.constructor.sealBlock(data.constructor.currentBlock());

            Node trueProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_1);
            Node falseProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_0);


            thenBlock.addPredecessor(trueProj);
            data.constructor.switchBlock(thenBlock);
            data.constructor.sealBlock(thenBlock);
            ifTree.then().accept(this, data);
            Node jmpNode = data.constructor.newJmp(afterIf);
            data.constructor.sealBlock(data.constructor.currentBlock());

            if (ifTree.orElse().isPresent()) {
                elseBlock.addPredecessor(falseProj);
                data.constructor.switchBlock(elseBlock);
                data.constructor.sealBlock(elseBlock);
                ifTree.orElse().get().accept(this, data);
                Node elseJmp = data.constructor.newJmp(afterIf);
                afterIf.addPredecessor(elseJmp);
                data.constructor.sealBlock(data.constructor.currentBlock());
            } else {
                afterIf.addPredecessor(falseProj);
            }

            afterIf.addPredecessor(jmpNode);
            data.constructor.switchBlock(afterIf);
            data.constructor.sealBlock(afterIf);

            popSpan();

            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
            pushSpan(whileTree);
            data.constructor.sealBlock(data.constructor.currentBlock());

            Block loopCondition = new Block(data.constructor.graph(), "while_" + Math.abs(whileTree.hashCode()));
            Block loopBody = new Block(data.constructor.graph(), "while_body_" + Math.abs(whileTree.hashCode()));
            Block loopAfter = new Block(data.constructor.graph(), "while_after_" + Math.abs(whileTree.hashCode()));

            //Generate Condition
            Node jmpCondition = data.constructor.newJmp(loopCondition);
            loopCondition.addPredecessor(jmpCondition);
            data.constructor.switchBlock(loopCondition);
            data.constructor.pushLoopStart(loopCondition);
            Node condition = whileTree.condition().accept(this, data).orElseThrow();
            Node ifNode = data.constructor.newIfNode(condition, loopBody, loopAfter);
            //Generate true and false paths
            Node trueProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_1);
            Node falseProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_0);

            //Loop After Block
            loopAfter.addPredecessor(falseProj);
            data.constructor.pushLoopEnd(loopAfter);

            //Body Block
            loopBody.addPredecessor(trueProj);
            data.constructor.sealBlock(loopBody);
            data.constructor.switchBlock(loopBody);
            whileTree.body().accept(this, data);
            Node jmp = data.constructor.newJmp(loopCondition);
            data.constructor.sealBlock(data.constructor.currentBlock());
            loopCondition.addPredecessor(jmp);


            data.constructor.popLoopStart();
            data.constructor.sealBlock(loopCondition);
            data.constructor.popLoopEnd();
            data.constructor.sealBlock(loopAfter);


            data.constructor.switchBlock(loopAfter);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TernaryTree ternaryTree, SsaTranslation data) {
            Node condition = ternaryTree.condition().accept(this, data).orElseThrow();

            Block ternaryTrue = new Block(data.constructor.graph(), "ternary_true_" + Math.abs(ternaryTree.hashCode()));
            Block ternaryFalse = new Block(data.constructor.graph(), "ternary_false_" + Math.abs(ternaryTree.hashCode()));
            Block followBlock = new Block(data.constructor.graph(), "ternary_follow_" + Math.abs(ternaryTree.hashCode()));

            Node conditionalNode = data.constructor.newIfNode(condition, ternaryTrue, ternaryFalse);
            Node trueProj = data.constructor.newControlFlowProj(conditionalNode, ProjNode.SimpleProjectionInfo.CF_1);
            Node falseProj = data.constructor.newControlFlowProj(conditionalNode, ProjNode.SimpleProjectionInfo.CF_0);
            ternaryTrue.addPredecessor(trueProj);
            ternaryFalse.addPredecessor(falseProj);
            data.constructor.sealBlock(ternaryTrue);
            data.constructor.sealBlock(ternaryFalse);

            data.constructor.switchBlock(ternaryTrue);
            Node trueNode = ternaryTree.trueBranch().accept(this, data).orElseThrow();
            Node trueBlockExit = data.constructor.newJmp(followBlock);
            data.constructor.switchBlock(ternaryFalse);
            Node falseNode = ternaryTree.falseBranch().accept(this, data).orElseThrow();
            Node falseBlockExit = data.constructor.newJmp(followBlock);

            data.constructor.switchBlock(followBlock);
            followBlock.addPredecessor(trueBlockExit);
            followBlock.addPredecessor(falseBlockExit);
            data.constructor.sealBlock(followBlock);

            Phi phi = data.constructor.newPhi();
            phi.addPredecessor(trueNode);
            phi.addPredecessor(falseNode);

            return Optional.of(data.constructor.tryRemoveTrivialPhi(phi));
        }

        @Override
        public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
            pushSpan(forTree);
            if (forTree.definition().isPresent()) {
                forTree.definition().get().accept(this, data);
            }
            data.constructor.sealBlock(data.constructor.currentBlock());

            Block loopCondition = new Block(data.constructor.graph(), "for_" + Math.abs(forTree.hashCode()));
            Block step = new Block(data.constructor.graph(), "for_step" + Math.abs(forTree.hashCode()));
            Block loopBody = new Block(data.constructor.graph(), "for_body_" + Math.abs(forTree.hashCode()));
            Block loopAfter = new Block(data.constructor.graph(), "for_after_" + Math.abs(forTree.hashCode()));
            data.constructor.pushLoopEnd(loopAfter);
            data.constructor.pushLoopStart(step);

            //Jump Into
            Node entry = data.constructor.newJmp(loopCondition);
            loopCondition.addPredecessor(entry);

            //Generate Condition
            data.constructor.switchBlock(loopCondition);
            Node condition = forTree.condition().accept(this, data).orElseThrow();
            Node ifNode = data.constructor.newIfNode(condition, loopBody, loopAfter);
            //Generate true and false paths
            Node trueProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_1);
            Node falseProj = data.constructor.newControlFlowProj(ifNode, ProjNode.SimpleProjectionInfo.CF_0);

            //Loop After Block
            loopAfter.addPredecessor(falseProj);

            //Body Block
            loopBody.addPredecessor(trueProj);

            data.constructor.switchBlock(step);
            if (forTree.statement().isPresent()) {
                forTree.statement().get().accept(this, data);
            }
            Node backToLoop = data.constructor.newJmp(loopCondition);
            loopCondition.addPredecessor(backToLoop);

            data.constructor.switchBlock(loopBody);
            forTree.body().accept(this, data);
            step.addPredecessor(data.constructor.newJmp(step));


            data.constructor.sealBlock(loopCondition);
            data.constructor.sealBlock(step);
            data.constructor.sealBlock(loopBody);
            data.constructor.sealBlock(loopAfter);

            data.constructor.popLoopEnd();
            data.constructor.popLoopStart();

            data.constructor.switchBlock(loopAfter);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BitwiseNegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newXor(data.constructor.newConstInt(-1), node);
            popSpan();
            return Optional.of(res);

        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            pushSpan(continueTree);
            Node jmpNode = data.constructor.newJmp(data.constructor.getLoopStart());
            data.constructor.getLoopStart().addPredecessor(jmpNode);
            data.constructor.sealBlock(data.constructor.currentBlock());
            Block newBlock = new Block(data.constructor.graph(), "after_continue_" + Math.abs(continueTree.hashCode()));
            data.constructor.switchBlock(newBlock);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            pushSpan(breakTree);
            Node jmpNode = data.constructor.newJmp(data.constructor.getLoopEnd());
            data.constructor.getLoopEnd().addPredecessor(jmpNode);
            data.constructor.sealBlock(data.constructor.currentBlock());
            Block newBlock = new Block(data.constructor.graph(), "after_break_" + Math.abs(breakTree.hashCode()));
            data.constructor.switchBlock(newBlock);
            popSpan();
            return NOT_AN_EXPRESSION;
        }
    }
}
