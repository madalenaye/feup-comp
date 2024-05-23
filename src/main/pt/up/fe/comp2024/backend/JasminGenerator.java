package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import java.util.ArrayList;
import java.util.List;
import static pt.up.fe.comp2024.backend.JasminUtils.*;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    private final FunctionClassMap<TreeNode, String> generators;
    private final JasminInstructionGenerator instructionGenerator;
    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        JasminUtils.setOllirResult(ollirResult);

        reports = new ArrayList<>();
        code = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);

        this.instructionGenerator = new JasminInstructionGenerator(ollirResult);

    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {

        StringBuilder code = new StringBuilder();

        // generate class name
        String className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        var defaultConstructor = new StringBuilder();
        defaultConstructor.append(".method public <init>()V").append(NL).append(TAB).append("aload_0").append(NL).append(TAB);

        if (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")) {
            code.append(".super java/lang/Object").append(NL);
            defaultConstructor.append("invokespecial java/lang/Object/<init>()V");
        } else {
            code.append(".super ").append(classUnit.getSuperClass()).append(NL);
            defaultConstructor.append("invokespecial ").append(classUnit.getSuperClass()).append("/<init>()V");
        }

        var classFields = ollirResult.getOllirClass().getFields();
        for (var field : classFields) {
            String accessModifierName = field.getFieldAccessModifier().name();
            String newAccessModifierName = switch (accessModifierName) {
                case "PUBLIC" -> "public";
                case "PRIVATE" -> "private";
                case "DEFAULT" -> "";
                default -> throw new IllegalStateException("Unexpected value: " + accessModifierName);
            };
            String fieldName = field.getFieldName();
            String fieldType = ollirTypeToJasmin(field.getFieldType());
            code.append(".field ").append(newAccessModifierName).append(" ").append(fieldName).append(" ").append(fieldType).append(NL);
        }

        defaultConstructor.append(NL).append(TAB).append("return").append(NL).append(".end method").append(NL);
        code.append(defaultConstructor);

        for (var method : ollirResult.getOllirClass().getMethods()) {
            if (method.isConstructMethod()) continue;
            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {

        instructionGenerator.setMethod(method);

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
            String parameterType = ollirTypeToJasmin(parameter.getType());
            code.append(parameterType);
        }

        var returnType = ollirTypeToJasmin(method.getReturnType());
        code.append(")").append(returnType).append(NL);


        StringBuilder instructionCode = new StringBuilder();
        for (Instruction inst : method.getInstructions()) {

            for (var label : method.getLabels().entrySet()){
                if (label.getValue() == inst)
                    instructionCode.append(label.getKey()).append(":").append(NL);
            }
            String instCode = instructionGenerator.generate(inst);
            instructionCode.append(instCode);

            if (inst.getInstType() == InstructionType.CALL &&
                    ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID)
                instructionCode.append("pop").append(NL);
        }

        int stackSize = instructionGenerator.getMaxStackSize();
        code.append(TAB).append(".limit stack 99").append(NL);

        int localSize = getLocalSize(method);
        code.append(TAB).append(".limit locals ").append(localSize).append(NL);

        code.append(instructionCode);
        code.append(".end method").append(NL);

        return code.toString();
    }


}
