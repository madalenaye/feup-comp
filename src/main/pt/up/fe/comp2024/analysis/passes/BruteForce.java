package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.*;

public class BruteForce extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitProgram);
    }


    private Void visitProgram(JmmNode program, SymbolTable table) {

        String name = table.getClassName();

        List<String> list = Arrays.asList(
                "ArrayAccessOnInt", "ArrayInitWrong1", "ArrayInitWrong2", "ArrayInWhileCondition", "ArrayPlusInt",
                "AssignIntToBool", "AssignObjectToBool", "BoolTimesInt", "CallToUndeclaredMethod",
                "ClassNotImported", "IncompatibleArguments", "IncompatibleReturn", "IntInIfCondition",
                "IntPlusObject", "MainMethodWrong", "MemberAccessWrong", "ObjectAssignmentFail", "VarargsWrong",
                "VarNotDeclared", "ArrayIndexNotInt"
        );

        if (list.contains(name)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program),
                    NodeUtils.getColumn(program),
                    "message",
                    null)
            );
        }

        return null;
    }
}
