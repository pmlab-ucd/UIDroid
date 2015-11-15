package uiDroid;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class TestTest {

	@Test
	public void test() {
		String sep = File.separator;
		String pathSep = File.pathSeparator;
		String path = System.getProperty("java.home") + sep + "lib" + sep
				+ "rt.jar";
		path += pathSep + "." + sep + "bin";
		Options.v().set_soot_classpath(path);
		// 载入MyClass类
		SootClass tgtClass = Scene.v().loadClassAndSupport("uiDroid.Test");
		// 把它作为我们要分析的类
		tgtClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		// 找到它的myMethod函数
		SootMethod method = tgtClass.getMethodByName("main");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		for (Unit unit: cfg) {
			Stmt stmt = (Stmt)unit;
			System.out.println(unit + " : ");
			if (stmt.containsInvokeExpr()) {
				//System.out.println(stmt.getInvokeExprBox());
				InvokeExpr ie = stmt.getInvokeExpr();
				System.out.println(ie.getMethod().getDeclaringClass());
			}
			
		}
	}

}
