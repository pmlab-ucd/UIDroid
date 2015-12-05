package playSoot;

import java.io.File;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Test {
	public static void main(String[] args) {
		String sep = File.separator;
		String pathSep = File.pathSeparator;
		String path = System.getProperty("java.home") + sep + "lib" + sep
				+ "rt.jar";
		path += pathSep + "." + sep + "bin";
		Options.v().set_soot_classpath(path);
		// 载入MyClass类
		SootClass tgtClass = Scene.v().loadClassAndSupport("playSoot.MyClass");
		// 把它作为我们要分析的类
		tgtClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		// 找到它的myMethod函数
		SootMethod method = tgtClass.getMethodByName("myMethod");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		// InitializedVarAnalysis.InitVarAnalysis an = new InitializedVarAnalysis.InitVarAnalysis(cfg);
		// iterate over the results
		for (Unit unit: cfg) {
			Stmt stmt = (Stmt)unit;
			if (stmt instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt)stmt;
				Value assigned = assignStmt.getLeftOp();
				System.out.println(assignStmt + ": " + assigned);

				JimpleLocal local = new JimpleLocal("$x5", assigned.getType());
				assignStmt.setLeftOp(local);
				System.out.println(assignStmt + ": " + local);
			}
		}
	}	

}
