package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.backend.regalloc.GraphColoringAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86CMP;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Command;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86CommutativeBinaryOperation;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Div;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Instruction;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86JE;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Jump;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86LShift;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Mov;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86MovConst;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86RShift;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Return;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Set;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Set.x86SetType;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Sub;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Test;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.node.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ComparisonNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.GreaterThanNode;
import edu.kit.kastel.vads.compiler.ir.node.GreaterThanOrEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.node.JmpNode;
import edu.kit.kastel.vads.compiler.ir.node.LShiftNode;
import edu.kit.kastel.vads.compiler.ir.node.LessThanNode;
import edu.kit.kastel.vads.compiler.ir.node.LessThanOrEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalAndNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalOrNode;
import edu.kit.kastel.vads.compiler.ir.node.LogicalUnequalNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.RShiftNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.node.XorNode;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {

    private final StackManager manager = new StackManager();
    private final HashMap<Block, BasicBlock> blockMap = new HashMap<>();
    private Block block = null;

    public String generateCode(List<List<Block>> program) throws IOException {
        StringBuilder builder = new StringBuilder();
        // Add template
        builder.append(new String(Files.readAllBytes(Paths.get("template_ass.s"))));


        for (List<Block> functionBlocks : program) {
            RegisterAllocator allocator = new GraphColoringAllocator(manager);
            Map<Node, Register> registers = allocator.allocateRegisters(functionBlocks);

            System.out.println("The stack is using: " + this.manager.getStackSize() + " bytes");
            if ( Main.DEBUG) System.out.println(registers);

            //TODO: Function handling
            builder.append("\n_")
                    .append("main")
                    .append(":\n");

            this.manager.construct(builder);
            for (Block block : functionBlocks) {
                BasicBlock cb = new BasicBlock(block);
                this.blockMap.put(block, cb);
            }

            for (Block block : functionBlocks) {
                this.block = block;
                block.nodesWithExitAndPhi().forEach(node ->
                    blockMap.get(block).command(parseNode(node, registers))
                );
            }

            for (Block block : functionBlocks) {
                builder.append(blockMap.get(block).print());
            }


            builder.append("\n");
        }

        return builder.toString();
    }

    private List<x86Instruction> parseNode(Node node, Map<Node, Register> registers) {
        return switch (node) {
            case ComparisonNode _ -> generateComp(node, registers);
            case BinaryOperationNode _ -> generateBinary(node, registers);
            case ReturnNode _ -> generateReturn(node, registers);
            case ConstIntNode c -> generateConst(c, registers);
            case ConstBoolNode c -> generateConst(c, registers);
            case ConditionalJumpNode i -> generateIf(i, registers);
            case Phi p -> generatePhi(p, registers);
            case JmpNode j -> List.of(new x86Jump(j.target().getLabel()));
            default -> List.of();
        };
    }

    private List<x86Instruction> generatePhi(Phi p, Map<Node, Register> registers) {
        if (p.isSideEffectPhi()) return List.of();
        Register src = registers.get(predecessorSkipProj(p, this.block.phiIndex(p)));
        Register target = registers.get(p);

        if (src == null || target == null) {
            System.err.println(predecessorSkipProj(p, this.block.phiIndex(p)));
            throw new IllegalArgumentException("Register Operands are not allowed to be null +"+ p + "("+ src + " " + target + ")");
        }

        return List.of(new x86Mov(src, HardwareRegister.EAX), new x86Mov(HardwareRegister.EAX
          , target));
    }

    private List<x86Instruction> generateIf(ConditionalJumpNode node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();
        Register condition = registers.get(node.getCondition());
        instructions.add(new x86Test(condition));
        instructions.add(new x86JE(node.getElseBlock().getLabel()));
        instructions.add(new x86Jump(node.getThenBlock().getLabel()));
        return instructions;
    }

    private List<x86Instruction> generateComp(Node node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        instructions.add(new x86CMP(op2, op1));
        x86SetType type = switch(node) {
            case LogicalEqualNode _ -> x86SetType.EQ;
            case LogicalUnequalNode _ -> x86SetType.NEQ;
            case LessThanNode _ -> x86SetType.LT;
            case LessThanOrEqualNode _ -> x86SetType.LTEQ;
            case GreaterThanNode _ -> x86SetType.GT;
            case GreaterThanOrEqualNode _ -> x86SetType.GTEQ;
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
        instructions.addAll(new x86Set(type, target).generate());

        return instructions;
    }

    private List<x86Instruction> generateReturn(Node node, Map<Node, Register> registers) {
        List<x86Instruction> instructions = new ArrayList<>();

        Register src = registers.get(predecessorSkipProj(node, ReturnNode.RESULT));
        instructions.add(new x86Mov(src, HardwareRegister.EAX));
        manager.destruct(instructions);
        instructions.add(new x86Return());

        return instructions;
    }

    private List<x86Instruction> generateConst(Node node, Map<Node, Register> registers) {
        return switch(node) {
            case ConstBoolNode b -> List.of(new x86MovConst(registers.get(b), b.value() ? 1 : 0));
            case ConstIntNode b -> List.of(new x86MovConst(registers.get(b), b.value()));
            default -> throw new UnsupportedOperationException();
        };
    }

    private List<x86Instruction> generateBinary(Node node, Map<Node, Register> registers) {
        Register op1 = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register op2 = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register target = registers.get(node);

        x86Instruction instruction = switch (node) {
            case AddNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.ADD);
            case MulNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.IMUL);
            case XorNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.XOR);
            case BitwiseAndNode _, LogicalAndNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.AND);
            case BitwiseOrNode _, LogicalOrNode _ -> new x86CommutativeBinaryOperation(op1, op2, target, x86Command.OR);
            case LShiftNode _ -> new x86LShift(op1, op2, target);
            case RShiftNode _ -> new x86RShift(op1, op2, target);
            case SubNode _ -> new x86Sub(op1, op2, target);
            case DivNode _ -> new x86Div(op1, op2, target, false);
            case ModNode _ -> new x86Div(op1, op2, target, true);
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };

        return instruction.generate();
    }
}
