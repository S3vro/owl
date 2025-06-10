package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.*;

class GraphConstructor {

    private final List<Optimizer> optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
    private Block currentBlock;

    public GraphConstructor(List<Optimizer> optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }

    public Node optimize(Node current) {
        Node node = current;
        for (Optimizer opt : this.optimizer) {
            node = opt.transform(node);
        }

        return node;
    }

    public void switchBlock(Block block) {
        this.currentBlock = block;
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }
    public Node newXor(Node left, Node right) {
        return this.optimize(new XorNode(currentBlock(), left, right));
    }
    public Node newBitWiseAnd(Node left, Node right) {
        return this.optimize(new BitwiseAndNode(currentBlock, left, right));
    }

    public Node newLogicalAnd(Node left, Node right) {
        return this.optimize(new LogicalAndNode(currentBlock, left, right));
    }
    public Node newBitwiseOr(Node left, Node right) {
        return this.optimize(new BitwiseOrNode(currentBlock, left, right));
    }
    public Node newLogicalOr(Node left, Node right) {
        return this.optimize(new LogicalOrNode(currentBlock, left, right));
    }
    public Node newLessThanNode(Node left, Node right) {
        return this.optimize(new LessThanNode(currentBlock, left, right));
    }
    public Node newLessThanOrEqualNode(Node left, Node right) {
        return this.optimize(new LessThanOrEqualNode(currentBlock, left, right));
    }
    public Node newLogicalEqual(Node left, Node right) {
        return this.optimize(new LogicalEqualNode(currentBlock, left, right));
    }

    public Node newLogicalUnequal(Node left, Node right) {
        return this.optimize(new LogicalUnequalNode(currentBlock, left, right));
    }
    public Node newAdd(Node left, Node right) {
        return this.optimize(new AddNode(currentBlock(), left, right));
    }
    public Node newSub(Node left, Node right) {
        return this.optimize(new SubNode(currentBlock(), left, right));
    }
    public Node newMul(Node left, Node right) {
        return this.optimize(new MulNode(currentBlock(), left, right));
    }

    public Node newLShiftNode(Node left, Node right) {
        return this.optimize(new LShiftNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimize(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMod(Node left, Node right) {
        return this.optimize(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newIfNode(Node exp, Block thenBlock, Block elseBlock) {
        return new IfNode(currentBlock(), exp, thenBlock, elseBlock);
    }

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numberin
        return this.optimize(new ConstIntNode(currentBlock(), value));
    }
    public Node newConstBool(boolean value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimize(new ConstBoolNode(this.graph.startBlock(), value));
    }

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newControlFlowProj(Node node, ProjNode.SimpleProjectionInfo type) {
        return new ProjNode(currentBlock(), node, type);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public Node newJmp(Block target) {
        return new JmpNode(currentBlock(), target);
    }

    public Node newUndef(List<? extends Node> node) {
        return new UndefNode(currentBlock(), (Node[]) node.toArray());
    }
    public Block currentBlock() {
        return this.currentBlock;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    public IrGraph graph() {
        return this.graph;
    }

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }


    // Implementation of Algorithm 2 from the SSA Paper
    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        Node same = null;
        for (Node op  : phi.predecessors()) {
            if (op.equals(same) || op.equals(phi)) {
                continue;
            }
            if(same != null) {
                return phi;
            }

            same = op;
        }
        if (same == null) {
            same = newUndef(phi.predecessors());
        }
        phi.replaceBy(same);
        for (Node use : phi.users()) {
            if (use instanceof Phi oPhi) {
                tryRemoveTrivialPhi(oPhi);
            }
        }
        return same;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }

        Phi sideEffectPhi = this.incompleteSideEffectPhis.get(block);
        if (sideEffectPhi != null) {
            addPhiOperands(sideEffectPhi);
        }

        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

}
