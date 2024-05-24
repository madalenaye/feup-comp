package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Verifies if there are undeclared variables, methods or classes.
 *
 */
public class DeclarationVerifier extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssign);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitAssign);
        addVisit(Kind.NEW_OBJECT_EXPR, this::visitObject);
        addVisit(Kind.OBJECT_TYPE, this::visitObject);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable table) {

        JmmNode object = methodExpr.getObject("object", JmmNode.class);
        Type type = TypeUtils.getExprType(object, table, currentMethod);

        if (type == null) {
            if (object.getKind().equals("VarRefExpr")) {
                String message = String.format("Class '%s' does not exist.", object.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(object),
                        NodeUtils.getColumn(object),
                        message,
                        null)
                );
            }
            return null;
        }

        // Object is from imported class, return
        if (table.getImports().contains(type.getName())) {
            return null;
        }

        if (type.getName().equals(table.getClassName())) {

            String method = methodExpr.get("method");

            // Method exists in current class, return
            if (table.getMethods().contains(method)) {
                return null;
            }

            // Method exists in super class (assume), return
            if (!table.getSuper().isBlank()) {
                return null;
            }

            String message = String.format("Method '%s' does not exist.", method);

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodExpr),
                    NodeUtils.getColumn(methodExpr),
                    message,
                    null)
            );
        }

        return null;
    }


    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        JmmNode parentNode = varRefExpr.getJmmParent();
        if (parentNode.getKind().equals("MethodExpr")) {
            return null;
        }

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        String varRefName = varRefExpr.get("name");

        // Var is a field, return
        if (table.getFields().stream().anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream().anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream().anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Create error report
        String message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }

    private Void visitObject(JmmNode expr, SymbolTable table) {

        String objectClass = expr.get("name");

        // Object is from current class, return
        if (objectClass.equals(table.getClassName())) {
            return null;
        }

        // Object is from imported class, return
        if (table.getImports().contains(objectClass)) {
            return null;
        }

        // Create error report
        String message = String.format("Class '%s' does not exist.", objectClass);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );

        return null;
    }

    private Void visitAssign(JmmNode stmt, SymbolTable table) {
        String variable = stmt.get("name");
        Type type = TypeUtils.getVariableType(variable, table, currentMethod);

        // If variable exists, return
        if (type != null) {
            return null;
        }

        // Create error report
        String message = String.format("Variable '%s' does not exist.", variable);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(stmt),
                NodeUtils.getColumn(stmt),
                message,
                null)
        );

        return null;
    }

}

