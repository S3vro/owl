package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.InferenceGraph;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

public class GraphColoringAllocator implements RegisterAllocator {

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
    private Map<InferenceGraph.InferenceGraphNode, Integer> colors = new HashMap<>();
    private final StackManager manager;

    public GraphColoringAllocator(StackManager manager) {
        this.manager = manager;
    }

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        InferenceGraph inGraph = new InferenceGraph();
        inGraph.generate(graph);
        InferenceGraph.InferenceGraphNode[] order = inGraph.maxCardinalitySearch();
        colors = inGraph.greedyColor(order);

        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited);
        return this.registers;
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
                if (!(predecessor instanceof Block) && visited.add(predecessor.block())) {
                    scan(predecessor.block(), visited);
                }
            }
        }
        if (needsRegister(node)) {
            int registerId = this.colors.get(new InferenceGraph.InferenceGraphNode(node));
            if (registerId < 11) {
                this.registers.put(node, HARDWARE_REGS.get(registerId));
            } else {
                // if no more registers spill
                StackRegister register = new StackRegister(registerId,0);
                this.manager.addToStack(register);
                this.registers.put(node, register);
            }

        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode || node instanceof JmpNode);
    }
}
