package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp2024.CompilerConfig;
import pt.up.fe.comp2024.optimization.ConstFoldVisitor;

import java.util.Collections;

public class AstToJasminImpl implements AstToJasmin {
    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {

        var generator = new JasminGeneratorVisitor(semanticsResult.getSymbolTable());
        var code = generator.visit(semanticsResult.getRootNode());

        return new JasminResult(semanticsResult, code, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (!CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            return semanticsResult;
        }

        ConstFoldVisitor constFoldVisitor = new ConstFoldVisitor();
        constFoldVisitor.visit(semanticsResult.getRootNode());

        return AstToJasmin.super.optimize(semanticsResult);
    }


}
