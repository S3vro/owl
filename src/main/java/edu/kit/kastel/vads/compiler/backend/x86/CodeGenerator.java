package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86CMP;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Command;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86CommutativeBinaryOperation;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Div;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Instruction;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86JE;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Jump;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86LShift;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Mov;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86MovConst;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Return;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Set;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Set.x86SetType;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Sub;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Test;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.node.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ComparisonNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.IfNode;
import edu.kit.kastel.vads.compiler.ir.node.JmpNode;
import edu.kit.kastel.vads.compiler.ir.node.LShiftNode;
import edu.kit.kastel.vads.compiler.ir.node.LessThanNode;
import edu.kit.kastel.vads.compiler.ir.node.LessThanOrEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalAndNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalOrNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalUnequalNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.node.XorNode;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CodeGenerator {

    private final StackManager manager = new StackManager();

    private Map<Block, BasicBlock> blocks = new HashMap<>();

    public String generateCode(List<IrGraph> program) throws IOException {
        StringBuilder builder = new StringBuilder();
        // Add template
        builder.append(new String(Files.readAllBytes(Paths.get("template_ass.s"))));


        for (IrGraph graph : program) {
            RegisterAllocator allocator = new GraphColoringAllocator(manager);
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            System.out.println("The stack is using: " + this.manager.getStackSize() + " bytes");

            builder.append("\n_")
                    .append(graph.name())
                    .append(":\n");

            this.manager.construct(builder);

            groupInstructionsPerBlock(graph, registers);
            List<BasicBlock> codeBlocks = orderBlocks(graph.endBlock()).reversed();
            for (BasicBlock block : codeBlocks) {
                builder.append(block.print());
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    private List<x86Instruction> parseNode(Node node, Map<Node, Register> registers) {
        return switch (node) {
            case ComparisonNode _ -> generateComp(node, registers);
            case BinaryOperationNode _ -> generateBinary(node, registers);
            case ReturnNode _ -> generateReturn(node, registers);
            case ConstIntNode c -> generateConst(c, registers);
            case ConstBoolNode c -> generateConst(c, registers);
            case IfNode i -> generateIf(i, registers);
            case Phi p -> generatePhi(p, registers);
            case JmpNode j -> List.of(new x86Jump(j.getTarget().getLabel()));
            default -> List.of();
        };
    }

    private List<x86Instruction> generatePhi(Phi p, Map<Node, Register> registers) {
        for (int i = 0; i < p.predecessors().size(); i++) {
            Node pred = predecessorSkipProj(p, i);
            Node predBlock = p.block().predecessors().get(i).block();
            Register src = registers.get(pred);
            Register target = registers.get(p);
            BasicBlock predBasicBlock = this.blocks.get(predBlock);
            predBasicBlock.addPhiInstruction(new x86Mov(src, HardwareRegister.EAX));
            predBasicBlock.addPhiInstruction(new x86Mov(HardwareRegister.EAX, target));
        }

        return List.of();
    }

    private List<x86Instruction> generateIf(IfNode node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();
        Register condition = registers.get(node.getCondition());
        instructions.add(new x86Test(condition));
        instructions.add(new x86JE(node.getElseBlock().getLabel()));
        instructions.add(new x86Jump(node.getThenBlock().getLabel()));
        return instructions;
    }

    private List<x86Instruction> generateComp(Node node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        instructions.add(new x86CMP(op1, op2));
        switch(node) {
            case LogicalEqualNode _ -> instructions.add(new x86Set(x86SetType.EQ, target));
            case LogicalUnequalNode _ -> instructions.add(new x86Set(x86SetType.NEQ, target));
            case LessThanNode _ -> instructions.add(new x86Set(x86SetType.LT, target));
            case LessThanOrEqualNode _ -> instructions.add(new x86Set(x86SetType.LTEQ, target));
            default -> {}
        };

        return instructions;
    }

    private List<x86Instruction> generateReturn(Node node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();

        Register src = registers.get(predecessorSkipProj(node, ReturnNode.RESULT));
        instructions.add(new x86Mov(src, HardwareRegister.EAX));
        manager.destruct(instructions);
        instructions.add(new x86Return());

        return instructions;
    }

    private List<x86Instruction> generateConst(Node node, Map<Node, Register> registers) {
        return switch(node) {
            case ConstBoolNode b -> List.of(new x86MovConst(registers.get(b), b.value() ? 1 : 0));
            case ConstIntNode b -> List.of(new x86MovConst(registers.get(b), b.value()));
            default -> throw new UnsupportedOperationException();
        };
    }

    private List<x86Instruction> generateBinary(Node node, Map<Node, Register> registers) {
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        x86Instruction instruction = switch (node) {
            case AddNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.ADD);
            case MulNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.IMUL);
            case XorNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.XOR);
            case BitwiseAndNode _, LogicalAndNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.AND);
            case BitwiseOrNode _, LogicalOrNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.OR);
            case LShiftNode _ -> new x86LShift(op1, op2, target);
            case SubNode _ -> new x86Sub(op1, op2, target);
            case DivNode _ -> new x86Div(op1, op2, target, false);
            case ModNode _ -> new x86Div(op1, op2, target, true);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };

        return instruction.generate();
    }

    private void groupInstructionsPerBlock(IrGraph graph, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited, registers);

    }

    private void scan(Node node, Set<Node> visited, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, registers);
            }
        }
        if (visited.add(node.block()))
            scan(node.block(), visited, registers);

        if (!ignore(node)) {
            BasicBlock block = this.blocks.computeIfAbsent(node.block(), BasicBlock::new);
            block.command(parseNode(node, registers));
        }
    }

    private boolean ignore(Node node) {
        return node instanceof ProjNode || node instanceof StartNode || node instanceof Block;
    }

    /* Implementation of Toposort for Block ordering */
    public List<BasicBlock> orderBlocks(Block endBlock) {
        List<BasicBlock> L = new ArrayList<>();
        List<BasicBlock> S = new ArrayList<>();
        S.add(blocks.computeIfAbsent(endBlock, _ -> new BasicBlock(endBlock)));

        while (!S.isEmpty()) {
            BasicBlock n = S.getFirst();
            if (!L.contains(n))
                L.add(n);
            S.removeFirst();

            // all pred blocks
            Set<BasicBlock> preds = n.getBlock().predecessors().stream().map(Node::block).map(b -> blocks.get(b))
             .collect(Collectors.toSet());
            S.addAll(preds);
        }

        return L;
    }

}
