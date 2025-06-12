package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackManager;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<Node, Register> allocateRegisters(List<Block> blocks) {
        for (Block block : blocks) {
            for (Node node : block.nodes()) {
                if (needsRegister(node)) {
                    if (this.id < 11) {
                        this.registers.put(node, HARDWARE_REGS.get(this.id++));
                    } else {
                        // if no more registers spill
                        int stackId = this.id++;
                        StackRegister register = new StackRegister(stackId, 0);
                        this.stackManager.addToStack(register);
                        this.registers.put(node, register);
                    }


                }
            }
        }

        return this.registers;
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }

}
