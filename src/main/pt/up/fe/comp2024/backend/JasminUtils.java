package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class JasminUtils {

    public static final String NL = "\n";
    public static final String TAB = "   ";

    private static OllirResult ollirResult;

    public static void setOllirResult(OllirResult ollirResult) {
        JasminUtils.ollirResult = ollirResult;
    }

    public static String getImportedClassName(String basicClassName) {

        if (basicClassName.equals("this")) {
            return ollirResult.getOllirClass().getClassName();
        }

        String fullClass = "." + basicClassName;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(basicClassName)){
            for (var imp: ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(fullClass)) return normalizeClassName(imp);
            }
        }

        return basicClassName;
    }

    public static String ollirTypeToJasmin(Type type) {
        if (type instanceof ArrayType arrayType) {
            ElementType elementType = arrayType.getElementType().getTypeOfElement();
            return switch (elementType) {
                case STRING -> "[Ljava/lang/String;";
                case INT32 -> "[I";
                default -> null;
            };
        }

        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "[Ljava/lang/String;";
            case OBJECTREF, CLASS -> getObjectType(type);
            case VOID -> "V";
            default -> null;
        };
    }

    public static String getBinaryOp(OperationType type) {
        return switch (type) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case AND -> "iand";
            case OR -> "ior";
            default -> throw new NotImplementedException(type);
        };
    }

    public static String getReturnType(ElementType type) {
        return switch (type) {
            case INT32, BOOLEAN -> "ireturn";
            case VOID -> "return";
            case OBJECTREF, CLASS, ARRAYREF-> "areturn";
            default -> "";
        };
    }

    public static int getVariableRegister(Method method, String variableName) {
        var variables = method.getVarTable();
        return variables.get(variableName).getVirtualReg();
    }

    public static int getLocalSize(Method method) {
        var variables = method.getVarTable();
        int max = 0;
        for (var variable : variables.values()) {
            max = Math.max(max, variable.getVirtualReg());
        }
        return max + 1;
    }

    private static String normalizeClassName(String className) {
        return className.replace(".", "/");
    }

    private static String getObjectType (Type type){
        return "L" + getImportedClassName(((ClassType) type).getName()) + ";";
    }



}
