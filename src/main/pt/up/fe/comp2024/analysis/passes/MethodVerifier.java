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


public class MethodVerifier extends AnalysisVisitor {

    private String currentMethod;
    private boolean hasMainMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {

        String message;

        // Superclass must be imported
        String superclass = table.getSuper();
        if (!superclass.isEmpty() && !table.getImports().contains(superclass)) {
            message = String.format("Superclass '%s' not imported", superclass);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(classDecl),
                    NodeUtils.getColumn(classDecl),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {

        currentMethod = method.get("name");

        boolean isStatic = Boolean.parseBoolean(method.get("isStatic"));
        boolean isPublic = Boolean.parseBoolean(method.get("isPublic"));

        List<Symbol> params = table.getParameters(currentMethod);

        String message;

        if (currentMethod.equals("main")) {
            
            if (!(isStatic && isPublic)) {
                message = "Main method must be public and static";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }

            if (params.size() == 1) {
                Type type = params.get(0).getType();
                if (type.getName().equals("String") && type.isArray()) {
                    return null;
                }
            }

            message = "Main method must have exactly one parameter (String[] args)";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        // other methods
        if (isStatic) {
            message = "Only main method can be static";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        for (Symbol param : params) {
            Type type = param.getType();
            if (type.isArray() && !type.getName().equals("int")) {
                message = "Only int[] arrays are allowed";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        JmmNode typeNode = varDecl.getChild(0);
        if (!typeNode.getKind().equals("ObjectArrayType")) {
            return null;
        }

        String message = "Only int[] arrays are allowed";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varDecl),
                NodeUtils.getColumn(varDecl),
                message,
                null)
        );
        
        return null;
    }

    private Void visitThisExpr(JmmNode expr, SymbolTable table) {
        if (!currentMethod.equals("main")) {
            return null;
        }
        
        String message = "'this' expression cannot be used in a static method";
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
