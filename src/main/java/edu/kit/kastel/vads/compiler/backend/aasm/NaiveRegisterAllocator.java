package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NaiveRegisterAllocator implements RegisterAllocator {

    private static final List<Register> HARDWARE_REGS = List.of(
            HardwareRegister.EBX,
            HardwareRegister.ECX,
            HardwareRegister.ESI,
            HardwareRegister.EDI,
            HardwareRegister.R8D,
            HardwareRegister.R9D,
            HardwareRegister.R10D,
            HardwareRegister.R11D,
            HardwareRegister.R12D,
            HardwareRegister.R13D,
            HardwareRegister.R14D

    );

    private final Map<Node, Register> registers = new HashMap<>();
    private int id = 0;
    private final StackManager stackManager;

    public NaiveRegisterAllocator(StackManager stackManager) {
        this.stackManager = stackManager;
    }

    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited);
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            if (this.id < 11) {
                this.registers.put(node, HARDWARE_REGS.get(this.id++));
            } else {
                System.out.println(node);
                // if no more registers spill
                int stackId = this.id++;
                this.stackManager.addToStack(new StackRegister(stackId));
                this.registers.put(node, new StackRegister(stackId));
            }

        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }

}
