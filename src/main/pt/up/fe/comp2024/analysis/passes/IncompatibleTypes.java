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

public class IncompatibleTypes  extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_STMT, this::visitCondition);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode expr, SymbolTable table) {

        Type type1 = TypeUtils.getExprType(expr.getChild(0), table);
        Type type2 = TypeUtils.getExprType(expr.getChild(1), table);

        if (!type1.equals(type2)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Incompatible types",
                    null)
            );
        }

        if (type1.isArray() && type2.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Arithmetic operation with arrays",
                    null)
            );
            return null;
        }


        return null;
    }


    private Type getVariableType(String variable, SymbolTable table) {
        for (var field : table.getFields()) {
            if (field.getName().equals(variable)) {
                return field.getType();
            }
        }
        for (var parameter : table.getParameters(currentMethod)) {
            if (parameter.getName().equals(variable)) {
                return parameter.getType();
            }
        }
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(variable)) {
                return local.getType();
            }
        }
        return null;
    }

    private Void visitAssignStmt(JmmNode stmt, SymbolTable table) {
        String variable = stmt.get("name");
        Type assignee = getVariableType(variable, table);

        JmmNode expr = stmt.getChild(0);
        Type assigned = TypeUtils.getExprType(expr, table);

        assert assignee != null;
        if (!assignee.equals(assigned)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    "Incompatible assignee and assigned types",
                    null)
            );
        }
        return null;
    }

    private Void visitCondition(JmmNode stmt, SymbolTable table) {
        JmmNode condition = stmt.getObject("condition", JmmNode.class);
        if (!TypeUtils.getExprType(condition, table).equals(new Type("boolean", false))) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(stmt),
                    NodeUtils.getColumn(stmt),
                    "Incompatible conditional type (expected boolean)",
                    null)
            );
        }
        return null;
    }

}

