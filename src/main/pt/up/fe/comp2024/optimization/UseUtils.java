package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class UseUtils {
    public static HashMap<Node, HashSet<String>> uses(List<Instruction> instructions) {
        HashMap<Node, HashSet<String>> map = new HashMap<>();
        for (Instruction instruction : instructions) {
            map.put(instruction, use(instruction));
        }
        return map;
    }

    private static HashSet<String> useAssign(AssignInstruction instruction) {
        return use(instruction.getRhs());
    }

    private static HashSet<String> useBinaryOper(BinaryOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();

        var leftOperand = instruction.getLeftOperand();
        var rightOperand = instruction.getRightOperand();

        if (leftOperand instanceof Operand leftOp) {
            set.add(leftOp.getName());
        }

        if (rightOperand instanceof Operand rightOp) {
            set.add(rightOp.getName());
        }

        return set;
    }

    private static HashSet<String> useUnaryOper(UnaryOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getOperand();
        if (operand instanceof Operand op) {
            set.add(op.getName());
        }
        return set;
    }

    private static HashSet<String> useCall(CallInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        CallType callType = instruction.getInvocationType();

        if (callType.equals(CallType.NEW)) {
            return set;
        }

        if (callType.equals(CallType.invokespecial)) {
            Element object = instruction.getCaller();
            if (object instanceof Operand obj && !obj.getName().equals("this")) {
                set.add(obj.getName());
            }
        }

        var arguments = instruction.getArguments();
        for (Element argument : arguments) {
            if (argument instanceof Operand arg) {
                set.add(arg.getName());
            }
        }
        return set;
    }

    private static HashSet<String> useReturn(ReturnInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getOperand();
        if (operand instanceof Operand op && !op.getName().equals("this")) {
            set.add(op.getName());
        }
        return set;
    }

    private static HashSet<String> useBranch(CondBranchInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operands = instruction.getOperands();
        for (var operand : operands) {
            if (operand instanceof Operand op && !op.getName().equals("this")) {
                set.add(op.getName());
            }
        }
        return set;
    }

    private static HashSet<String> useGetField(GetFieldInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operands = instruction.getOperands();
        for (Element operand : operands) {
            if (operand instanceof Operand op && !op.getName().equals("this")) {
                set.add(op.getName());
            }
        }
        return set;
    }

    private static HashSet<String> usePutField(PutFieldInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getValue();
        if (operand instanceof Operand op) {
            set.add(op.getName());
        }
        return set;
    }

    private static HashSet<String> useGoto(GotoInstruction instruction) {
        HashSet<String> set = new HashSet<>();

        return set;
    }

    private static HashSet<String> useNoOper(SingleOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getSingleOperand();
        if (operand instanceof Operand op) {
            set.add(op.getName());
        }
        return set;
    }

    private static HashSet<String> use(Instruction instruction) {
        InstructionType type = instruction.getInstType();
        if (type.equals(InstructionType.ASSIGN)) return useAssign((AssignInstruction) instruction);
        else if (type.equals(InstructionType.BINARYOPER)) return useBinaryOper((BinaryOpInstruction) instruction);
        else if (type.equals(InstructionType.BRANCH)) return useBranch((CondBranchInstruction) instruction);
        else if (type.equals(InstructionType.CALL)) return useCall((CallInstruction) instruction);
        else if (type.equals(InstructionType.GETFIELD)) return useGetField((GetFieldInstruction) instruction);
        else if (type.equals(InstructionType.GOTO)) return useGoto((GotoInstruction) instruction);
        else if (type.equals(InstructionType.NOPER)) return useNoOper((SingleOpInstruction) instruction);
        else if (type.equals(InstructionType.PUTFIELD)) return usePutField((PutFieldInstruction) instruction);
        else if (type.equals(InstructionType.RETURN)) return useReturn((ReturnInstruction) instruction);
        else if (type.equals(InstructionType.UNARYOPER)) return useUnaryOper((UnaryOpInstruction) instruction);
        return new HashSet<>();
    }

}
