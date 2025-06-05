package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Instruction;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86JE;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Jump;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Return;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BasicBlock {

    private final List<x86Instruction> instructions = new ArrayList<>();
    private final Map<Integer, String> comments = new HashMap<>();
    private final List<x86Instruction> phis = new ArrayList<>();
    private final Block block;
    private String label;

    public BasicBlock(Block block) {
        this.block = block;
        this.label = this.block.getLabel();
    }

    public String getLabel() {
        return label;
    }

    public Block getBlock() {
        return block;
    }

    public void comment(String comment) {
    this.comments.put(this.instructions.size(), comment);
    }

    public void comment(int line, String comment) {
        this.comments.put(line, comment);
    }
    public void command(x86Instruction cmd) {
        instructions.add(cmd);
    }

    public void command(List<? extends x86Instruction> cmd) {
        instructions.addAll(cmd);
    }

    public void label(String label) {
        this.label = label;
    }

    public void addPhiInstruction(x86Instruction phi) {
        this.phis.add(phi);

    }

    private void printInstruction(StringBuilder builder, x86Instruction instruction) {
        String comment = this.comments.get(instructions.indexOf(instruction));
        if (comment != null) {
            builder.append('#').append(comment).append('\n');
        }
        builder.append('\t').append(instruction);
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.label).append(':').append('\n');
        for (x86Instruction instruction : instructions.stream().filter(inst -> !this.isBlockEnd(inst)).toList()) {
            printInstruction(builder, instruction);
        }

        for (x86Instruction instruction : phis) {
            printInstruction(builder, instruction);
        }

        for (x86Instruction instruction : instructions.stream().filter(this::isBlockEnd).toList()) {
            printInstruction(builder, instruction);
        }

        return builder.toString();
    }

    private boolean isBlockEnd(x86Instruction instruction) {
        return instruction instanceof x86Return || instruction instanceof x86JE || instruction instanceof x86Jump;
    }
}
