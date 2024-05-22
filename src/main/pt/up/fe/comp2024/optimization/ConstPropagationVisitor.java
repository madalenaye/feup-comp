package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

public class ConstPropagationVisitor extends AJmmVisitor<SymbolTable, Boolean>  {

    // def
    // gen
    // use
    // kill

    @Override
    public void buildVisitor() {
        //addVisit(Kind.VAR_DECL, this::visitVarDecl);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitBinaryExpr(JmmNode expr, SymbolTable table) {

        return true;
    }


    private Boolean defaultVisit(JmmNode node, SymbolTable table) {
        for(JmmNode child : node.getChildren()) {
            visit(child);
        }
        return true;
    }
}
