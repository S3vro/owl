package edu.kit.kastel.vads.compiler.backend.x86;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Instruction;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.JmpNode;

public final class CodeBlock {

    private final List<x86Instruction> instructions = new ArrayList<>();
    private final Block block;
    private String label;
    private JmpNode jmp;

    public CodeBlock(Block block) {
        this.block = block;
        this.label = this.block.getLabel();
    }

    public String getLabel() {
        return label;
    }

    public Block getBlock() {
        return block;
    }

    public void command(x86Instruction cmd) {
        instructions.add(cmd);
    }

    public void command(List<x86Instruction> cmd) {
        instructions.addAll(cmd);
    }

    public void jmp(JmpNode jmp) {
        this.jmp = jmp;
    }

    public void label(String label) {
        this.label = label;
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.label).append(':').append('\n');
        for (x86Instruction instruction : instructions) {
            builder.append('\t').append(instruction);
        }
        if (jmp != null)
            builder.append(jmp);
        return builder.toString();
    }
}
