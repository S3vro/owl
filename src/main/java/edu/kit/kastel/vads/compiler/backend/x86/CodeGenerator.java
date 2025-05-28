package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.*;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Set.x86SetType;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

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

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    private final StackManager manager = new StackManager();

    Map<Block, CodeBlock> blocks = new HashMap<>();

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
            List<CodeBlock> codeBlocks = orderBlocks(graph.endBlock()).reversed();
            for (CodeBlock block : codeBlocks) {
                builder.append(block.print());
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    private List<x86Instruction> parseNode(Node node, Map<Node, Register> registers) {
        return switch (node) {
            case ComparisonNode _ -> generateComp(node, registers);
            case AddNode _, SubNode _, MulNode _ , DivNode _, ModNode _, XorNode _, BitwiseAndNode _ -> generateBinary(node, registers);
            case ReturnNode _ -> generateReturn(node, registers);
            case ConstIntNode c -> generateConst(c, registers);
            case IfNode i -> generateIf(i, registers);
            default -> List.of();
        };
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

    private List<x86Instruction> generateConst(ConstIntNode node, Map<Node, Register> registers) {
        return List.of(new x86MovConst(registers.get(node), node.value()));
    }

    private List<x86Instruction> generateBinary(Node node, Map<Node, Register> registers) {
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        x86Instruction instruction = switch (node) {
            case AddNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.ADD);
            case MulNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.IMUL);
            case XorNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.XOR);
            case BitwiseAndNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.AND);
            case SubNode _ -> new x86Sub(op1, op2, target);
            case DivNode _ -> new x86Div(op1, op2, target, false);
            case ModNode _ -> new x86Div(op1, op2, target, true);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };

        return instruction.generate();
    }

    private List<Node> groupInstructionsPerBlock(IrGraph graph, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        List<Node> order = new ArrayList<>();
        scan(graph.endBlock(), visited, order, registers);

        return order;
    }

    private void scan(Node node, Set<Node> visited, List<Node> order, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, order, registers);
                if (!(predecessor instanceof Block) && visited.add(node.block()))
                    scan(node.block(), visited, order, registers);
            }

        }
        if (!ignore(node)) {
            CodeBlock block = this.blocks.computeIfAbsent(node.block(), b -> new CodeBlock(b));
            if (node instanceof JmpNode jmp) {
                block.jmp(jmp);
            } else {
                block.command(parseNode(node, registers));
            }
        }
    }

    private boolean ignore(Node node) {
        return node instanceof ProjNode || node instanceof StartNode || node instanceof Block;
    }

    /* Implementation of Toposort for Block ordering */
    public List<CodeBlock> orderBlocks(Block endBlock) {
        List<CodeBlock> L = new ArrayList<>();
        List<CodeBlock> S = new ArrayList<>();
        S.add(blocks.computeIfAbsent(endBlock, _ -> new CodeBlock(endBlock)));

        while (!S.isEmpty()) {
            CodeBlock n = S.get(0);
            if (!L.contains(n))
                L.add(n);
            S.remove(0);

            // all pred blocks
            Set<CodeBlock> preds = n.getBlock().predecessors().stream().map(Node::block).map(b -> blocks.get(b)).collect(Collectors.toSet());
            for (CodeBlock b : preds) {
                S.add(b);
            }
        }
    
        return L;
    }

}
