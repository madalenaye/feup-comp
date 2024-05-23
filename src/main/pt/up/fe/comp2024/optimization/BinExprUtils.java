package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BinExprUtils {
    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final SymbolTable table;
    private String currMethod;

    private String ollirType;
    public BinExprUtils(SymbolTable table,String currMethod,String ollirType) {
        this.table = table;
        this.currMethod=currMethod;
        this.ollirType=ollirType;
    }
    public OllirExprResult exprHandler(JmmNode node,String code)  {
        if (node.getKind().equals("MethodExpr")) {
            return methodExprHandler(node,code);
        } else if (node.getKind().equals("ArrayElemExpr")) {
            return arrayElemExprHandler(node,code);
        } else {
            return new OllirExprResult("","");
        }

    }

    public OllirExprResult methodExprHandler(JmmNode node,String code) {
        StringBuilder computation = new StringBuilder();

        if (Objects.requireNonNull(TypeUtils.getExprType(node, table, currMethod)).hasAttribute("isExternal")) {
            code = code.substring(0, code.lastIndexOf(".")) + ollirType + ";\n";
        }
        String newTmp = OptUtils.getTemp() + ollirType;
        computation.append(newTmp)
                .append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                .append(code);
        code = newTmp;

        return new OllirExprResult(code,computation.toString());
    }

    public OllirExprResult arrayElemExprHandler(JmmNode node,String code) {
        StringBuilder computation = new StringBuilder();

        String newTmp = OptUtils.getTemp() + ollirType;
            computation.append(newTmp)
                    .append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append(code).append(";\n");
            code = newTmp;

        return new OllirExprResult(code,computation.toString());
    }

    public OllirExprResult shortCircuit(JmmNode left, OllirExprResult lhs, JmmNode right, OllirExprResult rhs){
        if(!right.getKind().equals("MethodExpr")){
            return new OllirExprResult("","");
        }
        StringBuilder computation = new StringBuilder();
        computation.append(lhs.getComputation());

        String temp = OptUtils.getTemp() + ".bool";

        String ifNumber= OptUtils.getIf();
        String endIfNumber= OptUtils.getEndIf();

        computation.append("if(").append(lhs.getCode()).append(") goto ").append(ifNumber).append(";\n");

        computation.append(temp).append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append("0.bool").append(";\n");
        computation.append("goto ").append(endIfNumber).append(";\n");
        computation.append(ifNumber).append(":\n");

        computation.append(rhs.getComputation());
        computation.append(temp).append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append(rhs.getCode());

        computation.append(endIfNumber).append(":\n");

        String code= temp;

        return new OllirExprResult(code,computation.toString());
    }
}
