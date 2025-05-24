package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.*;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    private final StackManager manager = new StackManager();

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

            generateForGraph(graph, builder, registers);

            builder.append("\n");
        }
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode _, SubNode _, MulNode _ , DivNode _, ModNode _ -> generateBinary(builder, node, registers);
            case ReturnNode r -> generateReturn(builder, r, registers);
            case ConstIntNode c -> generateConst(builder, c, registers);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
    }

    private void generateReturn(StringBuilder builder, Node node, Map<Node, Register> registers) {
        Register src = registers.get(predecessorSkipProj(node, ReturnNode.RESULT));
        new x86Mov(src, HardwareRegister.EAX).appendInstruction(builder);
        manager.destruct(builder);
        builder.append("ret").append('\n');
    }

    private void generateConst(StringBuilder builder, ConstIntNode node, Map<Node, Register> registers) {
        builder.append("mov ")
        .append(registers.get(node))
        .append(", ")
        .append(node.value())
        .append('\n');
    }

    private void generateBinary(StringBuilder builder, Node node, Map<Node, Register> registers) {
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        x86Instruction instruction = switch (node) {
            case AddNode _ -> new x86Add(op1, op2, target);
            case SubNode _ -> new x86Sub(op1, op2, target);
            case MulNode _ -> new x86Mul(op1, op2, target);
            case DivNode _ -> new x86Div(op1, op2, target, false);
            case ModNode _ -> new x86Div(op1, op2, target, true);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };

        instruction.appendInstruction(builder);
    }
}
