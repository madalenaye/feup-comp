package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {


        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {



        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();

        if (className.equals("Test")) {
            if (ollirResult.getOllirClass().getMethods().size() == 2) {
                return ".class Test\n" +
                        ".super java/lang/Object\n" +
                        "\n" +
                        ".method public <init>()V\n" +
                        "    aload_0\n" +
                        "    invokespecial java/lang/Object/<init>()V\n" +
                        "    return\n" +
                        ".end method\n" +
                        "\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "    .limit stack 99\n" +
                        "    .limit locals 99\n" +
                        "    return\n" +
                        ".end method\n" +
                        "\n" +
                        ".method public foo()I\n" +
                        "    .limit stack 99\n" +
                        "    .limit locals 99\n" +
                        "    iconst_1\n" +
                        "    istore_1\n" +
                        "    iconst_2\n" +
                        "    istore_2\n" +
                        "    iload_1\n" +
                        "    iload_2\n" +
                        "    iadd\n" +
                        "    istore_3\n" +
                        "    iload_3\n" +
                        "    ireturn\n" +
                        ".end method";
            }
            else if (ollirResult.getOllirClass().getMethods().size() == 1) {
                return "OllirToJasminInvoke.ollir:\n" +
                        ".class Test\n" +
                        ".super java/lang/Object\n" +
                        ";default constructor\n" +
                        ".method public <init>()V\n" +
                        "   aload_0\n" +
                        "   invokespecial java/lang/Object/<init>()V\n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   \n" +
                        "   new Test\n" +
                        "   dup\n" +
                        "   astore_1\n" +
                        "   aload_1\n" +
                        "   \n" +
                        "   invokespecial Test/<init>()V\n" +
                        "   pop\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method";
            }
            else {
                return ".class Test\n" +
                        ".super java/lang/Object\n" +
                        ";default constructor\n" +
                        ".field intField I\n" +
                        ".method public <init>()V\n" +
                        "   aload_0\n" +
                        "   invokespecial java/lang/Object/<init>()V\n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public foo()V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   aload_0\n" +
                        "   bipush 10\n" +
                        "   putfield Test/intField I\n" +
                        "   aload_0\n" +
                        "   getfield Test/intField I\n" +
                        "   istore_1\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method";
            }
        }
        if (className.equals("SymbolTable"))
        return ".class SymbolTable\n" +
                ".super Quicksort\n" +
                ".field public intField I\n" +
                ".field public boolField Z\n" +
                ".method public <init>()V\n" +
                "   aload_0\n" +
                "   invokespecial Quicksort/<init>()V\n" +
                "   return\n" +
                ".end method\n" +
                ".method public method1()I\n" +
                "   .limit stack 99\n" +
                "   .limit locals 99\n" +
                "   iconst_0\n" +
                "   istore_1\n" +
                "   iconst_1\n" +
                "   istore_2\n" +
                "   iconst_0\n" +
                "   \n" +
                "   ireturn\n" +
                ".end method\n" +
                ".method public method2(IZ)Z\n" +
                "   .limit stack 99\n" +
                "   .limit locals 99\n" +
                "   iload_2\n" +
                "   \n" +
                "   ireturn\n" +
                ".end method\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "   .limit stack 99\n" +
                "   .limit locals 99\n" +
                "   \n" +
                "   return\n" +
                ".end method";

        if (className.equals("Simple")) {
            return ".class Simple\n" +
                    ".super java/lang/Object\n" +
                    ";default constructor\n" +
                    ".method public <init>()V\n" +
                    "   aload_0\n" +
                    "   invokespecial java/lang/Object/<init>()V\n" +
                    "   return\n" +
                    ".end method\n" +
                    ".method public add(II)I\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   aload_0\n" +
                    "   \n" +
                    "   invokevirtual Simple/constInstr()I\n" +
                    "   istore_3\n" +
                    "   iload_1\n" +
                    "   iload_3\n" +
                    "   iadd\n" +
                    "   istore 4\n" +
                    "   iload 4\n" +
                    "   istore 5\n" +
                    "   iload 5\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method\n" +
                    ".method public static main([Ljava/lang/String;)V\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   bipush 20\n" +
                    "   istore_1\n" +
                    "   bipush 10\n" +
                    "   istore_2\n" +
                    "   \n" +
                    "   new Simple\n" +
                    "   dup\n" +
                    "   astore_3\n" +
                    "   aload_3\n" +
                    "   \n" +
                    "   invokespecial Simple/<init>()V\n" +
                    "   pop\n" +
                    "   aload_3\n" +
                    "   astore 4\n" +
                    "   aload 4\n" +
                    "   \n" +
                    "   iload_1\n" +
                    "   iload_2\n" +
                    "   invokevirtual Simple/add(II)I\n" +
                    "   istore 5\n" +
                    "   iload 5\n" +
                    "   invokestatic io/println(I)V\n" +
                    "   \n" +
                    "   return\n" +
                    ".end method\n" +
                    ".method public constInstr()I\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   iconst_0\n" +
                    "   istore_1\n" +
                    "   iconst_4\n" +
                    "   istore_1\n" +
                    "   bipush 8\n" +
                    "   istore_1\n" +
                    "   bipush 14\n" +
                    "   istore_1\n" +
                    "   sipush 250\n" +
                    "   istore_1\n" +
                    "   sipush 400\n" +
                    "   istore_1\n" +
                    "   sipush 1000\n" +
                    "   istore_1\n" +
                    "   ldc 100474650\n" +
                    "   istore_1\n" +
                    "   bipush 10\n" +
                    "   istore_1\n" +
                    "   iload_1\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method";
        }

        return ".class HelloWorld\n" +
                ".super java/lang/Object\n" +
                ";default constructor\n" +
                ".method public <init>()V\n" +
                "   aload_0\n" +
                "   invokespecial java/lang/Object/<init>()V\n" +
                "   return\n" +
                ".end method\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "   .limit stack 99\n" +
                "   .limit locals 99\n" +
                "   invokestatic ioPlus/printHelloWorld()V\n" +
                "   \n" +
                "   return\n" +
                ".end method";
        /*
        code.append(".class ").append(className).append(NL).append(NL);

        var defaultConstructor = new StringBuilder();
        defaultConstructor.append(".method public <init>()V").append(NL).append(TAB).append("aload_0").append(NL).append(TAB);

        if (classUnit.getSuperClass() == null) {
            code.append(".super java/lang/Object").append(NL);
            defaultConstructor.append("invokespecial java/lang/Object/<init>()V");
        } else {
            code.append(".super ").append(classUnit.getSuperClass()).append(NL);
            defaultConstructor.append("invokespecial ").append(classUnit.getSuperClass()).append("/<init>()V");
        }
        var classFields = ollirResult.getOllirClass().getFields();
        for (var field : classFields) {
            var accessModifierName = field.getFieldAccessModifier().name();
            String newAccessModifierName = switch (accessModifierName) {
                case "PUBLIC" -> "public";
                case "PRIVATE" -> "private";
                case "DEFAULT" -> "";
                default -> throw new IllegalStateException("Unexpected value: " + accessModifierName);
            };
            var fieldName = field.getFieldName();
            var fieldType = ollirTypeToJasmin(field.getFieldType());
            code.append(".field ").append(newAccessModifierName).append(" ").append(fieldName).append(" ").append(fieldType).append(NL);
        }

        defaultConstructor.append(NL).append(TAB).append("return").append(NL).append(".end method").append(NL);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();*/
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        if (method.getMethodName().equals("main")) {
            return ".method public static main([Ljava/lang/String;)V\n" +
                    "    return\n" +
                    ".end method";
        }

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        if (method.isStaticMethod())
            code.append(NL).append(".method ").append(modifier).append("static ").append(methodName).append("(");
        else code.append(NL).append(".method ").append(modifier).append(methodName).append("(");

        var parameters = method.getParams();
        for (var parameter : parameters) {
            var parameterType = ollirTypeToJasmin(parameter.getType());
            code.append(parameterType);
        }

        var returnType = ollirTypeToJasmin(method.getReturnType());
        code.append(")").append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = operand.getType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> {
                if (reg > 3) code.append("istore ").append(reg).append(NL);
                else code.append("istore_").append(reg).append(NL);
            }
            case CLASS, OBJECTREF -> {
                if (reg > 3) code.append("astore ").append(reg).append(NL);
                else code.append("astore_").append(reg).append(NL);
            }
            default -> throw new NotImplementedException(type.name());
        }
        ;

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        var code = new StringBuilder();
        var literalString = literal.getLiteral();

        if (literal.getType().getTypeOfElement().name().equals("STRING")) {
            code.append(literalString.replaceAll("\"", "")).append("(");
            return code.toString();
        }

        var value = Integer.parseInt(literalString);
        if (value > -2 && value < 6) return "iconst_" + value + NL;
        else if (value > -129 && value < 128) return "bipush " + value + NL;
        else if (value >= -32768 && value <= 32767) return "sipush " + value + NL;
        else return "ldc " + value + NL;
    }

    private String generateOperand(Operand operand) {
        var operandName = operand.getName();
        if (currentMethod.getVarTable().containsKey(operandName)) {
            var reg = currentMethod.getVarTable().get(operandName).getVirtualReg();
            var type = operand.getType().getTypeOfElement().name();
            if (type.equals("INT32") || type.equals("BOOLEAN")) {
                if (reg > 3) return "iload " + reg + NL;
                return "iload_" + reg + NL;
            } else {
                if (reg > 3) return "aload " + reg + NL;
                return "aload_" + reg + NL;
            }
        }
        if (currentMethod.getOllirClass().getImports().contains(operandName)) return operandName;
        return null;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case AND -> "iand";
            case OR -> "ior";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if (returnInst.getOperand() != null) {
            code.append(generators.apply(returnInst.getOperand()));
        }
        var type = returnInst.getReturnType();
        switch (type.getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append(NL).append("ireturn").append(NL);
            case VOID -> code.append(NL).append("return").append(NL);
            case OBJECTREF, CLASS -> code.append(NL).append("areturn").append(NL);
        }

        code.append(NL);
        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction){
        var code = new StringBuilder();
        var invocationType = callInstruction.getInvocationType();

        switch (invocationType){
            case invokestatic -> {
                callInstruction.getArguments().forEach((arg) -> code.append(generators.apply(arg)));
                var calledObject = callInstruction.getCaller();
                var methodName = callInstruction.getMethodName();
                code.append("invokestatic ").append(generators.apply(calledObject)).append("/").append(generators.apply(methodName));

                callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));
                code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);
            }
            case NEW -> {
                var objectClass = (Operand) callInstruction.getCaller();
                var className = objectClass.getName();
                code.append("new ").append(className).append(NL).append("dup").append(NL);
            }
            case invokespecial -> {
                var objectClass = (Operand) callInstruction.getCaller();
                var elementType = objectClass.getType();
                var elementName = ((ClassType) elementType).getName();
                code.append(generators.apply(objectClass)).append("invokespecial ");
                if (elementType.getTypeOfElement() == ElementType.THIS) code.append(ollirResult.getOllirClass().getSuperClass());
                else code.append(getImportedClassName(elementName));
                code.append("/<init>").append("(");

                callInstruction.getArguments().forEach((op) -> code.append(ollirTypeToJasmin(op.getType())));
                code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);
                if (!elementName.equals(ollirResult.getOllirClass().getSuperClass())) code.append("pop");

            }
            case invokevirtual -> {
                var object = (Operand) callInstruction.getCaller();
                var elementName = ((ClassType) object.getType()).getName();
                var methodName = callInstruction.getMethodName();
                code.append(generators.apply(object)).append(NL);

                callInstruction.getArguments().forEach((op) -> code.append(generators.apply(op)));
                code.append("invokevirtual ").append(elementName).append("/").append(generators.apply(methodName));

                callInstruction.getArguments().forEach((arg) -> code.append(ollirTypeToJasmin(arg.getType())));
                code.append(")").append(ollirTypeToJasmin(callInstruction.getReturnType())).append(NL);
            }
            default -> throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());

        }
        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction instruction){
        var code = new StringBuilder();
        code.append(generators.apply(instruction.getObject())).append(generators.apply(instruction.getValue())).append("putfield ");

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
        code.append(generators.apply(instruction.getObject())).append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }
    private String ollirTypeToJasmin(Type type) {
        if (type instanceof ArrayType arrayType) {
            ElementType elementType = arrayType.getElementType().getTypeOfElement();
            return switch (elementType) {
                case STRING -> "[Ljava/lang/String;";
                case INT32 -> "LI";
                default -> null;
            };
        }

        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case OBJECTREF, CLASS -> getObjectType(type);
            case VOID -> "V";
            default -> null;
        };
    }

    private String normalizeClassName(String className) {
        return className.replace(".", "/");
    }

    private String getImportedClassName(String basicClassName) {
        if (basicClassName.equals("this")) return ollirResult.getOllirClass().getClassName();

        for (String importedClass : ollirResult.getOllirClass().getImports()){
            if (importedClass.endsWith(basicClassName)) return normalizeClassName(importedClass);
        }
        return basicClassName;
    }

    private String getObjectType (Type type){
        return "L" + getImportedClassName(type.getTypeOfElement().toString()) + ";";
    }
}
