package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.stream.Collectors;
import static pt.up.fe.comp2024.backend.JasminUtils.*;

public class JasminInstructionGenerator {

    private final OllirResult ollirResult;
    private final FunctionClassMap<TreeNode, String> instructionGenerator;
    private final JasminOperandGenerator operandGenerator;
    private Method currentMethod;

    public JasminInstructionGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.operandGenerator = new JasminOperandGenerator(ollirResult);
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
    }

    public String generate(Instruction instruction) {
        String code = instructionGenerator.apply(instruction);
        return StringLines.getLines(code).stream().collect(Collectors.joining(NL + TAB, TAB, NL));
    }

    private String generateAssign(AssignInstruction assign) {
        StringBuilder code = new StringBuilder();

        String assignedCode = instructionGenerator.apply(assign.getRhs());
        code.append(assignedCode);

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = operand.getType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> {
                if (reg > 3) code.append("istore ").append(reg).append(NL);
                else code.append("istore_").append(reg).append(NL);
            }
            case CLASS, OBJECTREF, STRING -> {
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
        StringBuilder code = new StringBuilder();

        switch (callInstruction.getInvocationType()){
            case invokestatic -> {

                callInstruction.getArguments().forEach((arg) -> code.append(operandGenerator.generate(arg)));
                var calledObject = callInstruction.getCaller();
                var methodName = callInstruction.getMethodName();
                code.append("invokestatic ").append(JasminUtils.getImportedClassName(operandGenerator.generate(calledObject))).append("/").append(operandGenerator.generate(methodName));

                callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));
                code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);
            }
            case NEW -> {
                callInstruction.getArguments().forEach((obj) -> code.append(operandGenerator.generate(obj)));
                var objectClass = (Operand) callInstruction.getCaller();
                var className = objectClass.getName();
                var fullClassName = getImportedClassName(className);
                code.append("new ").append(fullClassName).append(NL).append("dup").append(NL);
            }
            case invokespecial -> {
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

            }
            case invokevirtual -> {
                var object = (Operand) callInstruction.getCaller();
                var elementName = ((ClassType) object.getType()).getName();
                var fullElementName = getImportedClassName(elementName);
                var methodName = callInstruction.getMethodName();
                code.append(operandGenerator.generate(object)).append(NL);

                callInstruction.getArguments().forEach((op) -> code.append(operandGenerator.generate(op)));
                code.append("invokevirtual ").append(fullElementName).append("/").append(operandGenerator.generate(methodName));

                callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));
                code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);

            }
            default -> throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());

        }
        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction instruction){
        var code = new StringBuilder();
        code.append(operandGenerator.generate(instruction.getObject())).append(operandGenerator.generate(instruction.getValue())).append("putfield ");

        var className = ((ClassType) instruction.getObject().getType()).getName();
        var fieldName = instruction.getField().getName();
        var fieldType = ollirTypeToJasmin(instruction.getField().getType());

        code.append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);
        return code.toString();
    }
    private String generateGetFieldInstruction(GetFieldInstruction instruction){
        var code = new StringBuilder();
        var className = ((ClassType) instruction.getObject().getType()).getName();
        var fieldName = instruction.getField().getName();
        var fieldType = ollirTypeToJasmin(instruction.getField().getType());
        code.append(operandGenerator.generate(instruction.getObject())).append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

}
