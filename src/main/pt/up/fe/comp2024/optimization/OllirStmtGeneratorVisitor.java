package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are statements.
 */
public class OllirStmtGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final SymbolTable table;
    private final OllirExprGeneratorVisitor exprVisitor;
    private String currMethod;

    public OllirStmtGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    public void setCurrMethod(String methodName) {
        this.currMethod = methodName;
        exprVisitor.setCurrMethod(currMethod);
    }

    @Override
    protected void buildVisitor() {
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        String variable = node.get("name");
        Type varType = TypeUtils.getVariableType(variable, table, currMethod);
        String varOllirType = OptUtils.toOllirType(varType);

        JmmNode exprNode = node.getJmmChild(0);
        var expr = exprVisitor.visit(exprNode);

        code.append(expr.getComputation());
        String exprCode = expr.getCode();

        if (exprNode.getKind().equals("MethodExpr")) {
            Type type = TypeUtils.getExprType(exprNode, table, currMethod);
            String newTmp = OptUtils.getTemp() + varOllirType;

            // static method call from imported class
            if (type.hasAttribute("isExternal")) {
                exprCode = exprCode.substring(0, exprCode.lastIndexOf(".")) + varOllirType + END_STMT;
            }

            code.append(newTmp)
                    .append(SPACE).append(ASSIGN).append(varOllirType).append(SPACE)
                    .append(exprCode);
            exprCode = newTmp;
        }

        if (table.getFields().stream().anyMatch(field -> field.getName().equals(variable))) {
            code.append("putfield(this, ").append(variable)
                    .append(varOllirType).append(", ").append(exprCode).append(").V");
        } else {
            code.append(variable).append(varOllirType);
            code.append(SPACE + ASSIGN).append(varOllirType).append(SPACE);
            code.append(exprCode);
        }

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        Type retType = table.getReturnType(currMethod);
        OllirExprResult expr = exprVisitor.visit(node.getJmmChild(0));

        code.append(expr.getComputation())
            .append("ret")
            .append(OptUtils.toOllirType(retType))
            .append(SPACE)
            .append(expr.getCode())
            .append(END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        var expr = exprVisitor.visit(node.getJmmChild(0));
        return expr.getComputation() + expr.getCode();
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }

}
