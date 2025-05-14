package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackManager;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import java.util.Map;

public final class ConstIntNode extends Node {
    private final int value;

    public ConstIntNode(Block block, int value) {
        super(block);
        this.value = value;
    }

    public void toASM(StringBuilder builder, Map<Node, Register> registers, StackManager manager) {
        Register target = registers.get(this);

        if (target instanceof HardwareRegister)  {
            builder.append("mov ")
                    .append("$")
                    .append(value)
                    .append(", ")
                    .append(registers.get(this));
        } else {
            builder.append("mov ")
                    .append("$")
                    .append(value)
                    .append(", ")
                    .append(manager.getOffset((StackRegister) target))
                    .append(String.format("(%s)", HardwareRegister.RSP));
        }
    }

    public int value() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstIntNode c) {
            return this.block() == c.block() && c.value == this.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    protected String info() {
        return "[" + this.value + "]";
    }
}
