package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

public class VarargsConverter extends AJmmVisitor<SymbolTable, Void>  {

    private String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.VARARG_TYPE, this::visitVarargType);
        setDefaultVisit(this::defaultVisit);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        if (hasVarargs(currentMethod, table)) {
            method.putObject("hasVararg", true);
        }

        for (JmmNode child : method.getChildren()) {
            visit(child, table);
        }

        return null;
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable table) {

        String method = methodExpr.get("method");

        if (hasVarargs(method, table)) {
            var formalParams = table.getParameters(method);
            var varargParams = methodExpr.getChildren(); varargParams.remove(0);
            if (formalParams.size() > 1) {
                varargParams.subList(0, formalParams.size() - 1).clear();
            }

            // if already passing varargs as array, return
            if (varargParams.size() == 1) {
                Type type = TypeUtils.getExprType(varargParams.get(0), table, currentMethod);
                if (type == null || type.isArray()) {
                    return null;
                }
            }

            // convert varargs to array
            JmmNode arrayExpr = new JmmNodeImpl("ArrayExpr");

            for (JmmNode varargParam : varargParams) {
                varargParam.detach();
                arrayExpr.add(varargParam);
            }

            methodExpr.add(arrayExpr);

        }

        return null;
    }

    boolean hasVarargs(String method, SymbolTable table) {

        if (!table.getMethods().contains(method)) {
            return false;
        }

        var parameters = table.getParameters(method);

        if (parameters.isEmpty()) {
            return false;
        }

        Type lastParameter = parameters.get(parameters.size() - 1).getType();
        return lastParameter.hasAttribute("isVararg");
    }

    private Void visitVarargType(JmmNode varargType, SymbolTable table) {
        JmmNode arrayType = new JmmNodeImpl("ArrayType");
        arrayType.put("name", "int");
        arrayType.putObject("isArray", true);
        varargType.replace(arrayType);
        return null;
    }

    private Void defaultVisit(JmmNode node, SymbolTable table) {
        for (JmmNode child : node.getChildren()) {
            visit(child, table);
        }
        return null;
    }


}
