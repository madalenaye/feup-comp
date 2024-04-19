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
import java.util.List;

public class ArrayVerifier extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayStmt);
        addVisit(Kind.ARRAY_ELEM_EXPR, this::visitArrayElemExpr);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpr);
        addVisit(Kind.NEW_ARRAY_EXPR, this::arrayIndexCheck);
        addVisit(Kind.LEN_EXPR, this::visitLenExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitArrayStmt(JmmNode arrayStmt, SymbolTable table) {

        String message;

        // Variable type validation
        String variable = arrayStmt.get("name");
        Type varType = TypeUtils.getVariableType(variable, table, currentMethod);

        if (varType == null) return null;
        if (!varType.getName().equals("int") || !varType.isArray()) {
            message = String.format("Array access on '%s' type", varType.getName());

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayStmt),
                    NodeUtils.getColumn(arrayStmt),
                    message,
                    null)
            );
        }

        // Array access index validation
        arrayIndexCheck(arrayStmt, table);

        // Assigned type check
        JmmNode assigned = arrayStmt.getObject("array", JmmNode.class);
        Type assignedType = TypeUtils.getExprType(assigned, table, currentMethod);

        if (assignedType == null) return null;
        if (TypeUtils.isIntType(assignedType)) {
            return null;
        }

        message = String.format("Array assignment expects an expression of type integer ('%s' given)", assignedType.getName() + (assignedType.isArray() ? "[]" : ""));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(arrayStmt),
                NodeUtils.getColumn(arrayStmt),
                message,
                null)
        );
        return null;
    }

    private Void visitArrayElemExpr(JmmNode expr, SymbolTable table) {

        // Array access index validation
        arrayIndexCheck(expr, table);

        // Variable type validation
        JmmNode variable = expr.getObject("array", JmmNode.class);
        Type varType = TypeUtils.getExprType(variable, table, currentMethod);

        if (varType == null) return null;
        if (varType.getName().equals("int") && varType.isArray()) {
            return null;
        }

        String message = String.format("Array access on '%s' type", varType.getName() + (varType.isArray() ? "[]" : ""));

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );


        return null;
    }

    private Void visitArrayExpr(JmmNode array, SymbolTable table) {

        List<JmmNode> elements = array.getChildren();
        for (JmmNode element : elements) {
            Type type = TypeUtils.getExprType(element, table, currentMethod);
            if (type == null) return null;
            if (!TypeUtils.isIntType(type)) {
                String message = String.format("Array expects elements of type integer ('%s' given)", type.getName() + (type.isArray() ? "[]" : ""));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(element),
                        NodeUtils.getColumn(element),
                        message,
                        null)
                );
                return null;
            }
        }

        return null;

    }

    private Void arrayIndexCheck(JmmNode expr, SymbolTable table) {
        JmmNode index = expr.getObject("index", JmmNode.class);
        Type indexType = TypeUtils.getExprType(index, table, currentMethod);

        if (indexType == null) return null;
        if (!TypeUtils.isIntType(indexType)) {
            String message = String.format("Array access index expects an expression of type integer ('%s' given)", indexType.getName() + (indexType.isArray() ? "[]" : ""));

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitLenExpr(JmmNode expr, SymbolTable table) {

        String length = expr.get("len");

        if (!length.equals("length")) {
            String message = "Incorrect usage of length expression";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    message,
                    null)
            );
        }

        Type varType = TypeUtils.getExprType(expr.getChild(0), table, currentMethod);

        if (varType == null || varType.isArray()) {
            return null;
        }

        String message = String.format("Array length access on '%s' type", varType.getName());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(expr),
                NodeUtils.getColumn(expr),
                message,
                null)
        );
        return null;
    }
}
