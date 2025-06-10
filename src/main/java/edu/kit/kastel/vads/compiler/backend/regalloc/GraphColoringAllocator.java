package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackManager;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.JmpNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphColoringAllocator implements RegisterAllocator {

    private static final List<Register> HARDWARE_REGS = List.of(
            HardwareRegister.EBX,
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
    public Map<Node, Register> allocateRegisters(List<Block> blocks) {
        InferenceGraph inGraph = new InferenceGraph();
        inGraph.generate(blocks);
        InferenceGraph.InferenceGraphNode[] order = inGraph.maxCardinalitySearch();
        colors = inGraph.greedyColor(order);

        for (Block block : blocks) {
            for (Node node : block.nodesWithExitAndPhi()) {
                if (needsRegister(node)) {
                    int registerId = this.colors.get(new InferenceGraph.InferenceGraphNode(node));
                    if (registerId < 10) {
                        this.registers.put(node, HARDWARE_REGS.get(registerId));
                    } else {
                        // if no more registers spill
                        StackRegister register = new StackRegister(registerId, 0);
                        this.manager.addToStack(register);
                        this.registers.put(node, register);
                    }
                }
            }
        }

        return this.registers;
    }

    private static boolean needsRegister(Node node) {
        if (node instanceof Phi phi) return !phi.isSideEffectPhi();
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof JmpNode);
    }

}
