package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

public class TypeVerifier extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_STMT, this::visitCondition);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
        addVisit(Kind.NEG_EXPR, this::visitNegExpr);
        addVisit(Kind.RETURN_STMT, this::visitReturn);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable table) {
        JmmNode object = methodExpr.getObject("object", JmmNode.class);
        Type objectType = TypeUtils.getExprType(object, table, currentMethod);

        if (objectType == null) {
            return null;
        }

        // External method, assume okay
        if (table.getImports().contains(objectType.getName())) {
            return null;
        }

        if (!TypeUtils.isObjectType(objectType)) {
            String message = String.format("Method call on non-object type ('%s' given)", objectType.getName() + (objectType.isArray() ? "[]" : ""));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(object),
                    NodeUtils.getColumn(object),
                    message,
                    null)
            );
            return null;
        }

        // Check argument types
        checkArguments(methodExpr, table);

        return null;
    }

    private void checkArguments(JmmNode methodExpr, SymbolTable table) {
        JmmNode object = methodExpr.getObject("object", JmmNode.class);
        Type objectType = TypeUtils.getExprType(object, table, currentMethod);

        String method = methodExpr.get("method");

        if (objectType == null) {
            return;
        }

        if (objectType.getName().equals(table.getClassName())) {

            // Call to method assumed in extends
            if (!table.getMethods().contains(method) && !table.getSuper().isEmpty()) {
                return;
            }

            String message;

            List<Symbol> expectedParams = table.getParameters(method);
            List<JmmNode> actualParams = methodExpr.getChildren(); actualParams.remove(0);

            // Method calls with no arguments
            if (expectedParams.isEmpty() && actualParams.isEmpty()) {
                return;
            }

            // Method calls with no arguments, but expected arguments
            if (expectedParams.isEmpty()) {
                message = String.format("Method call has wrong number of arguments (expected 0, given %d)", actualParams.size());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodExpr),
                        NodeUtils.getColumn(methodExpr),
                        message,
                        null)
                );
                return;
            }

            Type lastType = expectedParams.get(expectedParams.size()-1).getType();
            boolean hasVararg = lastType.hasAttribute("isVararg");

            if (!hasVararg && actualParams.size() != expectedParams.size()) {
                message = String.format("Method call has wrong number of arguments (expected %d, given %d)", expectedParams.size(), actualParams.size());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodExpr),
                        NodeUtils.getColumn(methodExpr),
                        message,
                        null)
                );
                return;
            }

            if (hasVararg && actualParams.size() < expectedParams.size() - 1) {
                message = String.format("Method call has fewer number of arguments than expected (expected %d, given %d)", expectedParams.size(), actualParams.size());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodExpr),
                        NodeUtils.getColumn(methodExpr),
                        message,
                        null)
                );
                return;
            }

            int i = 0;

            while (i < expectedParams.size() - 1) {
                Type actualType = TypeUtils.getExprType(actualParams.get(i), table, currentMethod);
                Type expectedType = expectedParams.get(i).getType();
                if (!expectedType.equals(actualType)) {
                    message = String.format("Incompatible argument types ('%s' expected, '%s' given)", expectedType.getName() + (expectedType.isArray() ? "[]" : ""), actualType.getName() + (actualType.isArray() ? "[]" : ""));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(actualParams.get(i)),
                            NodeUtils.getColumn(actualParams.get(i)),
                            message,
                            null)
                    );
                }
                i++;
            }

            if (!hasVararg) {
                Type actualType = TypeUtils.getExprType(actualParams.get(i), table, currentMethod);
                Type expectedType = expectedParams.get(i).getType();
                if (!expectedType.equals(actualType)) {
                    message = String.format("Incompatible argument types ('%s' expected, '%s' given)", expectedType.getName() + (expectedType.isArray() ? "[]" : ""), actualType.getName() + (actualType.isArray() ? "[]" : ""));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(actualParams.get(i)),
                            NodeUtils.getColumn(actualParams.get(i)),
                            message,
                            null)
                    );
                }
                return;
            }

            // Vararg argument set to empty array
            if (actualParams.size() < expectedParams.size()) {
                return;
            } 

            // Only one vararg argument, return is int[]
            if (actualParams.size() == expectedParams.size()) {
                Type actualType = TypeUtils.getExprType(actualParams.get(i--), table, currentMethod);
                assert actualType != null;
                if (actualType.getName().equals("int") && actualType.isArray()) {
                    return;
                }
            }

            // Vararg arguments, must be in int type
            for (int j = i; j < actualParams.size(); j++) {
                Type actualType = TypeUtils.getExprType(actualParams.get(j), table, currentMethod);
                assert actualType != null;
                if (!TypeUtils.isIntType(actualType)) {
                    message = String.format("Vararg argument must be of type 'int' ('%s' given)", actualType.getName() + (actualType.isArray() ? "[]" : ""));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(actualParams.get(j)),
                            NodeUtils.getColumn(actualParams.get(j)),
                            message,
                            null)
                    );
                }
            }
        }
    }

    private Void visitReturn(JmmNode returnStmt, SymbolTable table) {
        JmmNode expr = returnStmt.getChild(0);

        Type actualType = TypeUtils.getExprType(expr, table, currentMethod);
        Type expectedType = table.getReturnType(currentMethod);

        if (expectedType.equals(actualType)) {
            return null;
        }

        // external method
        if (actualType != null && table.getImports().contains(actualType.getName())) {
            return null;
        }

        String message = String.format("Method '%s' should return a '%s' type", currentMethod, expectedType.getName() + (expectedType.isArray() ? "[]" : ""));
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );

        return null;
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode expr, SymbolTable table) {

        Type type1 = TypeUtils.getExprType(expr.getChild(0), table, currentMethod);
        Type type2 = TypeUtils.getExprType(expr.getChild(1), table, currentMethod);
        String operator = expr.get("op");

        if (type1 == null || type2 == null) {
            return null;
        }

        String message;

        switch (operator) {
            case "+", "*", "-", "/", "<" -> {
                if (TypeUtils.isIntType(type1) && TypeUtils.isIntType(type2)) return null;
                message = String.format("'%s' operation expects two integers ('%s' and '%s' given)", operator, type1.getName() + (type1.isArray() ? "[]" : ""), type2.getName() + (type2.isArray() ? "[]" : ""));
            }
            case "&&"-> {
                if (TypeUtils.isBoolType(type1) && TypeUtils.isBoolType(type2)) return null;
                message = String.format("'%s' operation expects two booleans ('%s' and '%s' given)", operator, type1.getName() + (type1.isArray() ? "[]" : ""), type2.getName() + (type2.isArray() ? "[]" : ""));
            }
            default -> throw new RuntimeException("Unknown operator '" + operator);
        }

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );
        return null;
    }

    private Void visitAssignStmt(JmmNode stmt, SymbolTable table) {
        String variable = stmt.get("name");
        Type assignee = TypeUtils.getVariableType(variable, table, currentMethod);

        JmmNode expr = stmt.getChild(0);
        Type assigned = TypeUtils.getExprType(expr, table, currentMethod);

        if (assignee == null || assigned == null) {
            return null;
        }

        // Compatible types, return
        if (assignee.equals(assigned)) {
            return null;
        }

        // External method, assume okay
        if (table.getImports().contains(assigned.getName())) {
            return null;
        }

        // Assignee is a superclass of assigned, return
        if (assignee.getName().equals(table.getSuper()) && assigned.getName().equals(table.getClassName())) {
            return null;
        }

        // Assignee and assigned are imported classes, return
        if (table.getImports().contains(assigned.getName()) && table.getImports().contains(assignee.getName())) {
            return null;
        }   

        String message = String.format("Incompatible assignee and assigned types ('%s' expected, '%s' given)", assignee.getName() + (assignee.isArray() ? "[]" : ""), assigned.getName() + (assigned.isArray() ? "[]" : ""));
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(stmt),
                NodeUtils.getColumn(stmt),
                message,
                null)
        );
        return null;
    }

    private Void visitCondition(JmmNode stmt, SymbolTable table) {
        JmmNode condition = stmt.getObject("condition", JmmNode.class);

        Type type = TypeUtils.getExprType(condition, table, currentMethod);

        assert type != null;
        if (TypeUtils.isBoolType(type)) {
            return null;
        }

        String message = String.format("Conditional type expects a boolean ('%s' given)", type.getName() + (type.isArray() ? "[]" : ""));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(stmt),
                NodeUtils.getColumn(stmt),
                message,
                null)
        );
        return null;
    }

    private Void visitNegExpr(JmmNode negExpr, SymbolTable table) {
        JmmNode expr = negExpr.getChild(0);

        Type type = TypeUtils.getExprType(expr, table, currentMethod);

        assert type != null;
        if (TypeUtils.isBoolType(type)) {
            return null;
        }

        String message = String.format("Negation expression expects a boolean ('%s' given)", type.getName() + (type.isArray() ? "[]" : ""));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(negExpr),
                NodeUtils.getColumn(negExpr),
                message,
                null)
        );
        return null;
    }

}

