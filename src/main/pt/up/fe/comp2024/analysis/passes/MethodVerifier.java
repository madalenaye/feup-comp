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
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;


public class MethodVerifier extends AnalysisVisitor {

    private String currentMethod;
    private boolean hasMainMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(OBJECT_ARRAY_TYPE, this::visitObjectArrayType);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.VOID_TYPE, this::visitVoidType);
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
        List<JmmNode> returnStmts = method.getChildren(RETURN_STMT);

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

            checkMainArguments(method, table);

            // Check return stmt
            if (!returnStmts.isEmpty()) {
                message = "Main method cannot have return statements";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmts.get(0)),
                        NodeUtils.getColumn(returnStmts.get(0)),
                        message,
                        null)
                );
            }

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

        // Check parameters type
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

        if (returnStmts.size() != 1) {
            message = "Method must have exactly one return statement";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        JmmNode lastChild = method.getChild(method.getNumChildren() - 1);

        if (!returnStmts.get(0).equals(lastChild)) {
            message = "Return statement must be the last statement in the method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmts.get(0)),
                    NodeUtils.getColumn(returnStmts.get(0)),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitObjectArrayType(JmmNode typeNode, SymbolTable table) {
        if (currentMethod != null && currentMethod.equals("main")) {
            Optional<JmmNode> node = typeNode.getAncestor(METHOD_DECL);
            if (node.isPresent()) {
                JmmNode mainMethod = node.get();
                if (!mainMethod.getChildren(PARAM).isEmpty()) {
                    JmmNode firstParamType = mainMethod.getChild(1).getChild(0);
                    if (typeNode.equals(firstParamType)) {
                        return null;
                    }
                }
            }
        }

        String message = "Only int[] arrays are allowed";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(typeNode),
                NodeUtils.getColumn(typeNode),
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


    private void checkMainArguments(JmmNode mainMethod, SymbolTable table) {
        List<Symbol> params = table.getParameters(currentMethod);

        if (params.size() == 1) {
            Type type = params.get(0).getType();
            if (type.getName().equals("String") && type.isArray()) {
                return;
            }
        }

        String message = "Main method must have exactly one parameter (String[] args)";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(mainMethod),
                NodeUtils.getColumn(mainMethod),
                message,
                null)
        );
    }

    private Void visitVoidType(JmmNode voidNode, SymbolTable table) {

        if (currentMethod != null && currentMethod.equals("main")) {
            Optional<JmmNode> node = voidNode.getAncestor(METHOD_DECL);
            if (node.isPresent()) {
                JmmNode mainMethod = node.get();
                if (voidNode.equals(mainMethod.getChild(0))) {
                    return null;
                }
            }
        }

        String message = "Void type can only be used in main method";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(voidNode),
                NodeUtils.getColumn(voidNode),
                message,
                null)
        );

        return null;
    }
}