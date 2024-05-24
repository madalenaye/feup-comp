package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
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
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(STMTS, this::visitStmts);
        addVisit(ARRAY_ASSIGN_STMT,this::visitArrayAssignStmt);

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

        if(node.getJmmChild(0).getKind().equals("ArrayExpr")){
            return exprCode;
        }

        while (exprNode.getKind().equals("ParensExpr")) exprNode = exprNode.getChild(0);

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

        boolean isField = table.getFields().stream().anyMatch(field -> field.getName().equals(variable));
        boolean isLocal = table.getLocalVariables(currMethod).stream().anyMatch(local -> local.getName().equals(variable));
        boolean isParam = table.getParameters(currMethod).stream().anyMatch(param -> param.getName().equals(variable));

        if (isField && !(isLocal || isParam))  {
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
        String varOllirType = OptUtils.toOllirType(retType);

        JmmNode exprNode = node.getJmmChild(0);
        OllirExprResult expr = exprVisitor.visit(exprNode);
        String exprCode = expr.getCode();

        while(exprNode.getKind().equals("ParensExpr")) exprNode = exprNode.getChild(0);

        code.append(expr.getComputation());

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
            code.append("ret")
            .append(varOllirType)
            .append(SPACE)
            .append(exprCode)
            .append(END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        var expr = exprVisitor.visit(node.getJmmChild(0));
        return expr.getComputation() + expr.getCode();
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        var condition = exprVisitor.visit(node.getJmmChild(0));

        String ifNumber= OptUtils.getIf();
        String endIfNumber= OptUtils.getEndIf();


        code.append(condition.getComputation());
        code.append("if (").append(condition.getCode()).append(") ").append("goto ").append(ifNumber).append(END_STMT);

        var statement2 = this.visit(node.getJmmChild(2));
        code.append(statement2);
        code.append("goto ").append(endIfNumber).append(END_STMT);

        code.append(ifNumber).append(":\n");

        var statement1 = this.visit(node.getJmmChild(1));
        code.append(statement1);

        code.append(endIfNumber).append(":\n");

        return code.toString();
    }

    private String visitStmts(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        List<JmmNode> stmts = NodeUtils.getStmts(node);
        for (JmmNode stmt : stmts) {
            String stmtCode = this.visit(stmt);
            code.append(stmtCode);
        }

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String whileCond= OptUtils.getWhileCond();
        String whileLoop= OptUtils.getWhileLoop();
        String whileEnd= OptUtils.getWhileEnd();


        code.append(whileCond).append(":\n");

        var condition = exprVisitor.visit(node.getJmmChild(0));
        code.append(condition.getComputation());
        code.append("if (").append(condition.getCode()).append(") ").append("goto ").append(whileLoop).append(END_STMT);

        code.append("goto ").append(whileEnd).append(END_STMT);

        var statement = this.visit(node.getJmmChild(1));

        code.append(whileLoop).append(":\n").append(statement);

        code.append("goto ").append(whileCond).append(END_STMT);
        code.append(whileEnd).append(":\n");

        return code.toString();
    }

    private String visitArrayAssignStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        String variable = node.get("name");
        Type varType = TypeUtils.getVariableType(variable, table, currMethod);
        String varOllirType = OptUtils.toOllirType(varType);

        JmmNode left = node.getJmmChild(0);
        var lhs = exprVisitor.visit(left);
        JmmNode right = node.getJmmChild(1);
        var rhs = exprVisitor.visit(right);

        code.append(lhs.getComputation());
        String leftCode = lhs.getCode();
        String rightCode = rhs.getCode();

        while (left.getKind().equals("ParensExpr")) left = left.getChild(0);
        while (left.getKind().equals("ParensExpr")) left = left.getChild(1);

        if (left.getKind().equals("MethodExpr")) {
            Type type = TypeUtils.getExprType(left, table, currMethod);
            String newTmp = OptUtils.getTemp() + varOllirType;

            // static method call from imported class
            if (type.hasAttribute("isExternal")) {
                leftCode = leftCode.substring(0, leftCode.lastIndexOf(".")) + varOllirType + END_STMT;
            }

            code.append(newTmp)
                    .append(SPACE).append(ASSIGN).append(varOllirType).append(SPACE)
                    .append(leftCode);
            leftCode = newTmp;
        }

        if (right.getKind().equals("MethodExpr")) {
            Type type = TypeUtils.getExprType(right, table, currMethod);
            String newTmp = OptUtils.getTemp() + varOllirType;

            // static method call from imported class
            if (type.hasAttribute("isExternal")) {
                rightCode = rightCode.substring(0, rightCode.lastIndexOf(".")) + varOllirType + END_STMT;
            }

            code.append(newTmp)
                    .append(SPACE).append(ASSIGN).append(varOllirType).append(SPACE)
                    .append(rightCode);
            rightCode = newTmp;
        }


        code.append(variable).append("[").append(leftCode).append("]").append(varOllirType);
        code.append(SPACE + ASSIGN).append(varOllirType).append(SPACE);
        code.append(rightCode);

        code.append(END_STMT);

        return code.toString();
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
