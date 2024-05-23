package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.HashSet;

public class ConstPropagationVisitor extends AJmmVisitor<SymbolTable, Boolean>  {

    private HashMap<String, JmmNode> gen;
    private HashMap<String, JmmNode> prsv;
    private HashMap<String, JmmNode> in;
    private HashMap<String, JmmNode> out;

    private String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMethodDecl(JmmNode method, SymbolTable table) {

        currentMethod = method.get("name");

        var stmts = method.getChildren();
        stmts.remove(0);
        stmts.removeAll(method.getChildren("Param"));
        stmts.removeAll(method.getChildren("VarDecl"));

        for(JmmNode stmt : stmts) {
            initSets(table);
            visit(stmt, table);
        }

        return false;
    }

    private void initSets(SymbolTable table) {

        this.gen = new HashMap<>();
        this.prsv = new HashMap<>();

        for (var local : table.getLocalVariables(currentMethod)) {
            gen.put(local.getName(), new JmmNodeImpl("T"));
            prsv.put(local.getName(), new JmmNodeImpl("F"));
        }

        if (this.out == null) {
            this.in = new HashMap<>();
            this.out = new HashMap<>();
            for (var local : table.getLocalVariables(currentMethod)) {
                in.put(local.getName(), new JmmNodeImpl("T"));
            }
        } else {
            this.in = new HashMap<>(this.out);
        }

    }

    private Boolean visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        String assignee = assignStmt.get("name");
        JmmNode assigned = assignStmt.getChild(0);

        boolean hasChanged = visit(assigned, table);

        // local variable assignment
        if (gen.containsKey(assignee)) {

            // if assigned to a constant value
            if (assigned.getKind().equals("IntegerLiteral")) {
                gen.replace(assignee, assigned);
            }

            // assigned to a non constant value
            else {
                gen.replace(assignee, new JmmNodeImpl("F"));
            }

            prsv.replace(assignee, new JmmNodeImpl("T"));
        }

        out = meetSets(gen, joinSets(in, prsv));
        return hasChanged;


    }

    private HashMap<String, JmmNode> joinSets(HashMap<String, JmmNode> set1, HashMap<String, JmmNode> set2) {
        HashMap<String, JmmNode> result = new HashMap<>();

        for (var key : set1.keySet()) {

            String kind1 = set1.get(key).getKind();
            String kind2 = set2.get(key).getKind();

            if (kind1.equals(kind2)) {
                result.put(key, set1.get(key));
            } else if (kind1.equals("T") || kind2.equals("T")) {
                result.put(key, new JmmNodeImpl("T"));
            } else if (kind1.equals("F")) {
                result.put(key, set2.get(key));
            } else {
                result.put(key, set1.get(key));
            }
        }

        return result;
    }

    private HashMap<String, JmmNode> meetSets(HashMap<String, JmmNode> set1, HashMap<String, JmmNode> set2) {
        HashMap<String, JmmNode> result = new HashMap<>();

        for (var key : set1.keySet()) {

            String kind1 = set1.get(key).getKind();
            String kind2 = set2.get(key).getKind();

            if (kind1.equals(kind2)) {
                result.put(key, set1.get(key));
            } else if (kind1.equals("T") || kind1.equals("F")) {
                result.put(key, set2.get(key));
            } else if (kind2.equals("T") || kind2.equals("F")) {
                result.put(key, set1.get(key));
            } else {
                result.put(key, new JmmNodeImpl("T"));
            }
        }

        return result;
    }

    private Boolean visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");

        if (in.containsKey(varName)) {
            String kind = in.get(varName).getKind();
            if (!kind.equals("T") && !kind.equals("F")) {
                varRefExpr.replace(in.get(varName));
                return true;
            }
        }

        return false;
    }

    private Boolean defaultVisit(JmmNode node, SymbolTable table) {
        boolean hasChanged = false;
        for(JmmNode child : node.getChildren()) {
            hasChanged |= visit(child, table);
        }
        return hasChanged;
    }
}
