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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SymbolTableVerifier extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.PROGRAM, this::visitProgram);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        String message;
        // param duplicates
        Set<Symbol> set = new HashSet<>();
        for (Symbol i : table.getParameters(currentMethod)) {
            if (set.contains(i)) {
                message = "duplicate params";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
            set.add(i);
        }
        set.clear();
        for (Symbol i : table.getLocalVariables(currentMethod)) {
            if (set.contains(i)) {
                message = "duplicate locals";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
            set.add(i);
        }

        return null;
    }

    private Void visitProgram(JmmNode program, SymbolTable table) {

        String message;

        // imports duplicates
        Set<String> set = new HashSet<>();
        for (String i : table.getImports()) {
            if (set.contains(i) || i.equals(table.getClassName())) {
                message = "duplicate imports";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        message,
                        null)
                );
                return null;
            }
            set.add(i);
        }
        /*
        // field duplicates
        Set<Symbol> fields = new HashSet<Symbol>();
        for (Symbol i : table.getFields()) {
            if (fields.contains(i)) {
                message = "duplicate fields";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        message,
                        null)
                );
                return null;
            } else
                fields.add(i);
        }

        // method duplicates
        Set<String> methods = new HashSet<>();
        for (String i : table.getMethods()) {
            if (methods.contains(i)) {
                message = "duplicate method";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        message,
                        null)
                );
                return null;
            } else
                methods.add(i);
        }
        */


        return null;
    }
}
