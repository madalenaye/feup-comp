package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class UseUtils {
    public static HashMap<Node, HashSet<String>> uses(Method method, List<Instruction> instructions) {
        HashMap<Node, HashSet<String>> map = new HashMap<>();
        for (Instruction instruction : instructions) {
            map.put(instruction, use(method, instruction));
        }
        return map;
    }

    private static HashSet<String> useAssign(Method method, AssignInstruction instruction) {
        return use(method, instruction.getRhs());
    }

    private static HashSet<String> useBinaryOper(Method method, BinaryOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();

        var leftOperand = instruction.getLeftOperand();
        var rightOperand = instruction.getRightOperand();

        if (isLocalVariable(method, leftOperand)) {
            set.add(((Operand) leftOperand).getName());
        }

        if (isLocalVariable(method, rightOperand)) {
            set.add(((Operand) rightOperand).getName());
        }

        return set;
    }

    private static HashSet<String> useUnaryOper(Method method, UnaryOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getOperand();
        if (isLocalVariable(method, operand)) {
            set.add(((Operand) operand).getName());
        }
        return set;
    }

    private static HashSet<String> useCall(Method method, CallInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        CallType callType = instruction.getInvocationType();

        if (callType.equals(CallType.NEW)) {
            return set;
        }

        for (Element operand : instruction.getOperands()) {
            if (isLocalVariable(method, operand)) {
                set.add(((Operand) operand).getName());
            }
        }

        return set;
    }

    private static HashSet<String> useReturn(Method method, ReturnInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getOperand();
        if (isLocalVariable(method, operand) && instruction.hasReturnValue()) {
            set.add(((Operand) operand).getName());
        }
        return set;
    }

    private static HashSet<String> useBranch(Method method, CondBranchInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        for (Element operand : instruction.getOperands()) {
            if (isLocalVariable(method, operand)) {
                set.add(((Operand) operand).getName());
            }
        }
        return set;
    }

    private static HashSet<String> useGetField(Method method, GetFieldInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        for (Element operand : instruction.getOperands()) {
            if (isLocalVariable(method, operand)) {
                set.add(((Operand) operand).getName());
            }
        }
        return set;
    }

    private static HashSet<String> usePutField(Method method, PutFieldInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getValue();
        if (isLocalVariable(method, operand)) {
            set.add(((Operand) operand).getName());
        }
        return set;
    }

    private static HashSet<String> useGoto(Method method, GotoInstruction instruction) {
        return new HashSet<>();
    }

    private static HashSet<String> useNoOper(Method method, SingleOpInstruction instruction) {
        HashSet<String> set = new HashSet<>();
        var operand = instruction.getSingleOperand();
        if (isLocalVariable(method, operand)) {
            set.add(((Operand) operand).getName());
        }
        return set;
    }

    private static HashSet<String> use(Method method, Instruction instruction) {
        InstructionType type = instruction.getInstType();
        if (type.equals(InstructionType.ASSIGN)) return useAssign(method, (AssignInstruction) instruction);
        else if (type.equals(InstructionType.BINARYOPER)) return useBinaryOper(method, (BinaryOpInstruction) instruction);
        else if (type.equals(InstructionType.BRANCH)) return useBranch(method, (CondBranchInstruction) instruction);
        else if (type.equals(InstructionType.CALL)) return useCall(method, (CallInstruction) instruction);
        else if (type.equals(InstructionType.GETFIELD)) return useGetField(method, (GetFieldInstruction) instruction);
        else if (type.equals(InstructionType.GOTO)) return useGoto(method, (GotoInstruction) instruction);
        else if (type.equals(InstructionType.NOPER)) return useNoOper(method, (SingleOpInstruction) instruction);
        else if (type.equals(InstructionType.PUTFIELD)) return usePutField(method, (PutFieldInstruction) instruction);
        else if (type.equals(InstructionType.RETURN)) return useReturn(method, (ReturnInstruction) instruction);
        else if (type.equals(InstructionType.UNARYOPER)) return useUnaryOper(method, (UnaryOpInstruction) instruction);
        return new HashSet<>();
    }

    private static boolean isLocalVariable(Method method, Element element) {
        var varTable = method.getVarTable();
        if (element instanceof Operand op) {
            String name = op.getName();
            if (varTable.containsKey(name)) {
                Descriptor desc = varTable.get(name);
                return desc.getScope().equals(VarScope.LOCAL) && !name.equals("this") && !element.isLiteral();
            }
        }
        return false;
    }
}
