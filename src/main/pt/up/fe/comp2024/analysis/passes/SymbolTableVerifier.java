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

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.PROGRAM, this::visitProgram);
    }

    private <T> boolean hasDuplicate(List<T> list) {
        Set<T> set = new HashSet<>();
        for (T i : list) {
            if (set.contains(i))
                return true;
            set.add(i);
        }
        return false;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String currentMethod = method.get("name");
        String message;

        // duplicate parameters
        var parameters = table.getParameters(currentMethod).stream().map(Symbol::getName).toList();
        if (hasDuplicate(parameters)) {
            message = String.format("'%s' method has duplicate parameters", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        // duplicate local variables
        var locals = table.getLocalVariables(currentMethod).stream().map(Symbol::getName).toList();
        if (hasDuplicate(locals)) {
            message = String.format("'%s' method has duplicate local variables", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitProgram(JmmNode program, SymbolTable table) {

        String message;

        // duplicate import classes
        if (hasDuplicate(table.getImports())) {
            message = "Duplicated import classes";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program),
                    NodeUtils.getColumn(program),
                    message,
                    null)
            );
            return null;
        }

        // duplicate fields
        List<String> fields = table.getFields().stream().map(Symbol::getName).toList();

        if (hasDuplicate(fields)) {
            message = "Duplicated fields";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program),
                    NodeUtils.getColumn(program),
                    message,
                    null)
            );
            return null;
        }

        // method duplicates
        if (hasDuplicate(table.getMethods())) {
            message = "Duplicated methods";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program),
                    NodeUtils.getColumn(program),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }
}
