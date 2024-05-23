package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.HashMap;
import java.util.stream.Collectors;
import static pt.up.fe.comp2024.backend.JasminUtils.*;

public class JasminInstructionGenerator {

    private final OllirResult ollirResult;
    private final FunctionClassMap<TreeNode, String> instructionGenerator;
    private final JasminOperandGenerator operandGenerator;
    private Method currentMethod;
    private int stackSize;
    private int maxStackSize;

    public JasminInstructionGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.operandGenerator = new JasminOperandGenerator(ollirResult, this);
        this.instructionGenerator = new FunctionClassMap<>();
        instructionGenerator.put(AssignInstruction.class, this::generateAssign);
        instructionGenerator.put(CallInstruction.class, this::generateCallInstruction);
        instructionGenerator.put(SingleOpInstruction.class, this::generateSingleOp);
        instructionGenerator.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        instructionGenerator.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        instructionGenerator.put(BinaryOpInstruction.class, this::generateBinaryOp);
        instructionGenerator.put(ReturnInstruction.class, this::generateReturn);
    }

    public void setMethod(Method method){
        this.currentMethod = method;
        this.operandGenerator.setCurrentMethod(method);
        this.stackSize = 0;
        this.maxStackSize = 0;
    }

    public String generate(Instruction instruction) {
        String code = instructionGenerator.apply(instruction);
        return StringLines.getLines(code).stream().collect(Collectors.joining(NL + TAB, TAB, NL));
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public void pushToStack(){
        stackSize++;
        if (stackSize > maxStackSize) maxStackSize = stackSize;
    }

    public void popFromStack(int values){
        stackSize -= values;
    }

    private String generateAssign(AssignInstruction assign) {
        StringBuilder code = new StringBuilder();

        Operand lhs = (Operand) assign.getDest();

        String assignedCode = instructionGenerator.apply(assign.getRhs());

        // get register
        var reg = currentMethod.getVarTable().get(lhs.getName()).getVirtualReg();

        if (lhs instanceof ArrayOperand arrayOperand) {
            if (reg > 3) code.append("aload ").append(reg).append(NL);
            else code.append("aload_").append(reg).append(NL);
            pushToStack();
            code.append(operandGenerator.generate(arrayOperand.getIndexOperands().get(0)));

            code.append(assignedCode);
            code.append("iastore").append(NL);
            popFromStack(3);
            return code.toString();
        }

        code.append(assignedCode);

        var type = lhs.getType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> {
                popFromStack(1);
                if (reg > 3) code.append("istore ").append(reg).append(NL);
                else code.append("istore_").append(reg).append(NL);
            }
            case CLASS, OBJECTREF, STRING, ARRAYREF -> {
                popFromStack(1);
                if (reg > 3) code.append("astore ").append(reg).append(NL);
                else code.append("astore_").append(reg).append(NL);
            }

            case VOID -> {}
            default -> throw new NotImplementedException(type.name());
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return operandGenerator.generate(singleOp.getSingleOperand());
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        StringBuilder code = new StringBuilder();

        String leftCode = operandGenerator.generate(binaryOp.getLeftOperand());
        String rightCode = operandGenerator.generate(binaryOp.getRightOperand());
        code.append(leftCode).append(rightCode);

        String op = getBinaryOp(binaryOp.getOperation().getOpType());
        code.append(op).append(NL);

        popFromStack(1);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        StringBuilder code = new StringBuilder();

        if (returnInst.getOperand() != null) {
            code.append(operandGenerator.generate(returnInst.getOperand()));
        }

        String type = getReturnType(returnInst.getReturnType().getTypeOfElement());

        code.append(type).append(NL);
        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction){
        return switch (callInstruction.getInvocationType()){
            case invokestatic -> handleStaticCall(callInstruction);
            case NEW -> handleNewCall(callInstruction);
            case invokespecial -> handleSpecialCall(callInstruction);
            case invokevirtual -> handleVirtualCall(callInstruction);
            default -> throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());
        };
    }

    private String handleStaticCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();

        callInstruction.getArguments().forEach((arg) -> code.append(operandGenerator.generate(arg)));
        code.append("invokestatic ");

        Element caller = callInstruction.getCaller();
        String callerCode = operandGenerator.generate(caller);
        code.append(getImportedClassName(callerCode)).append("/");

        Element methodName = callInstruction.getMethodName();
        String methodCode = operandGenerator.generate(methodName);
        code.append(methodCode);

        callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));

        code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);

        return code.toString();
    }

    private String handleNewCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();

        callInstruction.getArguments().forEach((obj) -> code.append(operandGenerator.generate(obj)));

        Operand caller = (Operand) callInstruction.getCaller();

        if (caller.getType() instanceof ArrayType) {
            code.append("newarray int").append(NL);
            return code.toString();
        }


        String className = caller.getName();

        String fullClassName = getImportedClassName(className);
        code.append("new ").append(fullClassName).append(NL).append("dup").append(NL);

        pushToStack();
        pushToStack();

        return code.toString();
    }

    private String handleSpecialCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();

        var objectClass = (Operand) callInstruction.getCaller();
        var elementType = objectClass.getType();
        var elementName = ((ClassType) elementType).getName();
        code.append(operandGenerator.generate(objectClass)).append("invokespecial ");
        if (elementType.getTypeOfElement() == ElementType.THIS) code.append(ollirResult.getOllirClass().getSuperClass());
        else code.append(getImportedClassName(elementName));
        code.append("/<init>").append("(");

        callInstruction.getArguments().forEach((op) -> code.append(ollirTypeToJasmin(op.getType())));
        code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);
        code.append("pop");

        return code.toString();
    }

    private String handleVirtualCall(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();

        int s = stackSize;
        popFromStack(1);
        Operand object = (Operand) callInstruction.getCaller();
        String elementName = ((ClassType) object.getType()).getName();
        String fullElementName = getImportedClassName(elementName);
        code.append(operandGenerator.generate(object)).append(NL);

        Element methodName = callInstruction.getMethodName();
        callInstruction.getArguments().forEach((op) -> code.append(operandGenerator.generate(op)));
        code.append("invokevirtual ").append(fullElementName).append("/").append(operandGenerator.generate(methodName));

        callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));
        code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);

        popFromStack(callInstruction.getArguments().size());

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction instruction){
        StringBuilder code = new StringBuilder();

        Element object = instruction.getObject();
        String objectCode = operandGenerator.generate(object);
        code.append(objectCode);

        Element value = instruction.getValue();
        String valueCode = operandGenerator.generate(value);
        code.append(valueCode);

        String className = ((ClassType) instruction.getObject().getType()).getName();
        String fieldName = instruction.getField().getName();
        String fieldType = ollirTypeToJasmin(instruction.getField().getType());

        code.append("putfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        popFromStack(2);

        return code.toString();
    }
    private String generateGetFieldInstruction(GetFieldInstruction instruction){
        StringBuilder code = new StringBuilder();

        Element object = instruction.getObject();
        String objectCode = operandGenerator.generate(object);
        code.append(objectCode);

        String className = ((ClassType) instruction.getObject().getType()).getName();
        String fieldName = instruction.getField().getName();
        String fieldType = ollirTypeToJasmin(instruction.getField().getType());
        code.append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

}
