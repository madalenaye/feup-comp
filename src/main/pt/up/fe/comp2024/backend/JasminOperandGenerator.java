package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import static pt.up.fe.comp2024.backend.JasminUtils.*;

public class JasminOperandGenerator {
    private final OllirResult ollirResult;
    private final FunctionClassMap<TreeNode, String> operandGenerator;
    private Method currentMethod;

    public JasminOperandGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        this.operandGenerator = new FunctionClassMap<>();
        operandGenerator.put(LiteralElement.class, this::generateLiteral);
        operandGenerator.put(Operand.class, this::generateOperand);
    }

    public void setCurrentMethod(Method currentMethod) {
        this.currentMethod = currentMethod;
    }

    public String generate(TreeNode operand) {
        return operandGenerator.apply(operand);
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
        String fullClass = "." + operandName;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(operandName)){
            for (var imp: ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(fullClass)) return operandName;
            }
        }
        return null;
    }

}
