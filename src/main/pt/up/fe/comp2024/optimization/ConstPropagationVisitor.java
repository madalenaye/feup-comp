package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ConstPropagationVisitor extends AJmmVisitor<SymbolTable, Boolean>  {

    private HashMap<String, JmmNode> gen;
    private HashMap<String, JmmNode> prsv;
    private HashMap<String, JmmNode> in;
    private HashMap<String, JmmNode> out;
    private String currentMethod;
    private boolean inWhile;
    private boolean processingWhile;
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.STMTS, this::visitStmts);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMethodDecl(JmmNode method, SymbolTable table) {

        this.out = null;

        currentMethod = method.get("name");
        boolean hasChanged = false;

        for(JmmNode stmt : getStatements(method)) {
            initSets(table);
            hasChanged |= visit(stmt, table);
        }

        return hasChanged;
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

    private Boolean visitWhileStmt(JmmNode whileStmt, SymbolTable table) {

        var condition = whileStmt.getObject("condition", JmmNode.class);
        var stmts = whileStmt.getChildren(); stmts.remove(condition);
        var out1 = new HashMap<>(in);

        boolean hasChanged = false;

        this.processingWhile = true;

        for (JmmNode stmt : stmts) {
            initSets(table);
            visit(stmt, table);
        }

        this.processingWhile = false;

        var out2 = new HashMap<>(out);
        this.out = meetSets(out1, out2);

        this.inWhile = true;

        initSets(table);
        hasChanged |= visit(condition, table);

        for (JmmNode stmt : stmts) {
            initSets(table);
            hasChanged |= visit(stmt, table);
        }

        this.inWhile = false;

        return hasChanged;
    }

    private Boolean visitAssignStmt(JmmNode assignStmt, SymbolTable table) {

        if (this.inWhile) {
            return false;
        }

        String assignee = assignStmt.get("name");
        JmmNode assigned = assignStmt.getChild(0);

        boolean hasChanged = visit(assigned, table);

        // local variable assignment
        if (gen.containsKey(assignee)) {

            // if assigned to a constant value
            if (assigned.getKind().equals("IntegerLiteral") || assigned.getKind().equals("BooleanLiteral")) {
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

    private Boolean visitIfStmt(JmmNode ifStmt, SymbolTable table) {

        JmmNode condition = ifStmt.getObject("condition", JmmNode.class);
        boolean hasChanged = visit(condition, table);

        var initialIn = new HashMap<>(out);

        var ifBody = ifStmt.getChild(1);
        hasChanged |= visit(ifBody, table);
        var outIf = new HashMap<>(out);

        // reset out set for the else body
        out = new HashMap<>(initialIn);

        var elseBody = ifStmt.getChild(2);
        hasChanged |= visit(elseBody, table);
        var outElse = new HashMap<>(out);

        // meet the out sets of the if and else bodies
        out = meetSets(outIf, outElse);

        return hasChanged;
    }

    private Boolean visitStmts(JmmNode stmts, SymbolTable table) {
        boolean hasChanged = false;
        for(JmmNode stmt : stmts.getChildren()) {
            hasChanged |= visit(stmt, table);
        }
        return hasChanged;
    }

    private Boolean visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {

        if (this.processingWhile) {
            return false;
        }

        String varName = varRefExpr.get("name");

        if (in.containsKey(varName)) {
            String kind = in.get(varName).getKind();
            if (!kind.equals("T") && !kind.equals("F")) {
                var constant = in.get(varName);
                var parent = varRefExpr.getParent();
                if (parent.getKind().equals("IfStmt") || parent.getKind().equals("WhileStmt")) {
                    parent.putObject("condition", constant);
                }
                varRefExpr.replace(constant);
                return true;
            }
        }

        return false;
    }

    private HashMap<String, JmmNode> joinSets(HashMap<String, JmmNode> set1, HashMap<String, JmmNode> set2) {
        HashMap<String, JmmNode> result = new HashMap<>();

        for (var key : set1.keySet()) {

            String kind1 = set1.get(key).getKind();
            String kind2 = set2.get(key).getKind();

            if (kind1.equals("T") || kind2.equals("T")) {
                result.put(key, new JmmNodeImpl("T"));
            } else if (kind2.equals("F")) {
                result.put(key, set1.get(key));
            } else if (kind1.equals("F")) {
                result.put(key, set2.get(key));
            } else if (kind1.equals(kind2)) {
                String val1 = set1.get(key).get("value");
                String val2 = set2.get(key).get("value");
                if (val1.equals(val2)) {
                    result.put(key, set1.get(key));
                } else {
                    result.put(key, new JmmNodeImpl("T"));
                }
            } else {
                result.put(key, new JmmNodeImpl("F"));
            }

        }

        return result;
    }

    private HashMap<String, JmmNode> meetSets(HashMap<String, JmmNode> set1, HashMap<String, JmmNode> set2) {
        HashMap<String, JmmNode> result = new HashMap<>();

        for (var key : set1.keySet()) {

            String kind1 = set1.get(key).getKind();
            String kind2 = set2.get(key).getKind();

            if (kind1.equals("F") || kind2.equals("F")) {
                result.put(key, new JmmNodeImpl("F"));
            } else if (kind2.equals("T")) {
                result.put(key, set1.get(key));
            } else if (kind1.equals("T")) {
                result.put(key, set2.get(key));
            } else if (kind1.equals(kind2)) {
                String val1 = set1.get(key).get("value");
                String val2 = set2.get(key).get("value");
                if (val1.equals(val2)) {
                    result.put(key, set1.get(key));
                } else {
                    result.put(key, new JmmNodeImpl("F"));
                }
            } else {
                result.put(key, new JmmNodeImpl("T"));
            }
        }

        return result;
    }

    private List<JmmNode> getStatements(JmmNode method) {
        var stmts = method.getChildren();
        stmts.remove(0);
        stmts.removeAll(method.getChildren("Param"));
        stmts.removeAll(method.getChildren("VarDecl"));
        return stmts;

    }
    private Boolean defaultVisit(JmmNode node, SymbolTable table) {
        boolean hasChanged = false;
        for(JmmNode child : node.getChildren()) {
            hasChanged |= visit(child, table);
        }
        return hasChanged;
    }
}

