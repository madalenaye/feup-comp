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

import static pt.up.fe.comp2024.ast.Kind.*;


public class VarargsVerifier extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        JmmNode typeNode = varDecl.getChild(0);
        if (!typeNode.getKind().equals("VarargType")) {
           return null;
        }

        String message = "Variable and field declarations cannot be vararg";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(typeNode),
                NodeUtils.getColumn(typeNode),
                message,
                null)
        );
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        JmmNode expr = returnStmt.getChild(0);

        Type actualType = TypeUtils.getExprType(expr, table, currentMethod);
        if (actualType == null || !actualType.hasAttribute("isVararg")) {
            return null;
        }

        String message = "Method returns cannot be vararg";
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

        String message;

        List<JmmNode> params = method.getChildren(Kind.PARAM);

        for (int i = 0; i < params.size() - 1; i++) {
            JmmNode typeNode = params.get(i).getChild(0);
            if (typeNode.getKind().equals("VarargType")) {
                message = "Only one parameter can be vararg and it must be the last one";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(typeNode),
                        NodeUtils.getColumn(typeNode),
                        message,
                        null)
                );
            }
        }

        return null;
    }


}
