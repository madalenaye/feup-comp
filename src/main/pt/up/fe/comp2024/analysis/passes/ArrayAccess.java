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

public class ArrayAccess extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_ELEM_EXPR, this::visitArrayElemExpr);
    }

    private Void visitArrayElemExpr(JmmNode expr, SymbolTable table) {

        JmmNode array = expr.getObject("array", JmmNode.class);
        if (!array.getKind().equals("VarRefExpr")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Array access not over an array",
                    null)
            );
            return null;
        } else if (!TypeUtils.getExprType(array, table).isArray()){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Array access not over an array",
                    null)
            );
        }


        /* Index handling */
        JmmNode index = expr.getObject("index", JmmNode.class);
        if (!TypeUtils.getExprType(index, table).equals(new Type("int", false))) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Array access index is not an expression of type integer",
                    null)
            );
            return null;
        }
        return null;
    }

}
