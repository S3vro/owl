package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

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
            //System.out.println(GraphVizPrinter.print(graph));
            RegisterAllocator allocator = new NaiveRegisterAllocator(this.manager);
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
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> binary(builder, registers, mul, "imul");
            case DivNode div -> divOrMod(builder, registers, div, false);
            case ModNode mod -> divOrMod(builder, registers, mod, true);
            case ReturnNode r -> r.toASM(builder, registers, manager);
            case ConstIntNode c -> c.toASM(builder, registers, manager);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private void binary(
        StringBuilder builder,
        Map<Node, Register> registers,
        BinaryOperationNode node,
        String opcode
    ) {

        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);


        if (op1 instanceof StackRegister stackOp1) {
            this.manager.retrieve(builder, stackOp1, HardwareRegister.EAX);
        }

        if (op2 instanceof StackRegister stackOp2) {
            manager.retrieve(builder, stackOp2, HardwareRegister.R15D);
        }

        if (!op2.equals(target)) {
            Register localTarget = target;
            if (target instanceof StackRegister) {
                localTarget = HardwareRegister.R15D;
            }
            builder.append("mov ")
                    .append(op2 instanceof HardwareRegister ? op2 : HardwareRegister.R15D)
                    .append(", ")
                    .append(localTarget)
                    .append("\n");
        }

        builder.append(opcode)
                .append(' ')
                .append(op1 instanceof HardwareRegister ? op1 : HardwareRegister.EAX)
                .append(", ")
                .append(target instanceof HardwareRegister ? target : HardwareRegister.R15D);

        if (target instanceof StackRegister targetStack) {
            manager.store(builder, HardwareRegister.R15D, targetStack);
        }
    }

    private void divOrMod(StringBuilder builder, Map<Node,Register> registers, BinaryOperationNode node, boolean mod) {
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        builder.append("xor ")
                .append(HardwareRegister.EDX)
                .append(", ")
                .append(HardwareRegister.EDX)
                .append('\n');

        if (op1 instanceof StackRegister stackOp1) {
            this.manager.retrieve(builder, stackOp1, HardwareRegister.R15D);
        }

        if (op2 instanceof StackRegister stackOp2) {
            manager.retrieve(builder, stackOp2, HardwareRegister.EAX);
        } else {
            builder.append("mov ")
                    .append(op2)
                    .append(", ")
                    .append(HardwareRegister.EAX)
                    .append('\n');
        }

        builder.append("div ").append(op1 instanceof HardwareRegister ? op1 : HardwareRegister.R15D).append('\n');

        HardwareRegister resultRegister = mod ? HardwareRegister.EDX : HardwareRegister.EAX;

        if (target instanceof HardwareRegister) {
            builder.append("mov ")
                    .append(resultRegister)
                    .append(", ")
                    .append(target);
        } else {
            this.manager.store(builder, resultRegister, (StackRegister) target);
        }

    }
}
