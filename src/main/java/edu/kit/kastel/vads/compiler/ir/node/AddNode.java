package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.aasm.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.aasm.StackManager;
import edu.kit.kastel.vads.compiler.backend.aasm.StackRegister;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import java.util.Map;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class AddNode extends BinaryOperationNode {

    public AddNode(Block block, Node left, Node right) {
        super(block, left, right);
    }


    @SuppressWarnings("EqualsDoesntCheckParameterClass") // we do, but not here
    @Override
    public boolean equals(Object obj) {
        return commutativeEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return commutativeHashCode(this);
    }
}
