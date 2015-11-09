/*
 * Warning:
 * This can only be executed correctly when using SOOTCLASSES　
 * and put the /bin/ as External Class folder suggested as 
 * http://stackoverflow.com/questions/20282481/loading-java-class-files-for-soot-dynamically-in-eclipse.
 * Not working anymore if use soot-trunk.jar as lib
 */

package playSoot;

import java.io.File;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;

public class MyMain {
	public static void main(String[] args) {
		args = new String[] {"playSoot.MyClass"};
		
		if (args.length == 0) {
			System.out.println("Usage: java RunLiveAnalysis class_to_analyse");
			System.exit(0);
		}
		
		String sep = File.separator;
		String pathSep = File.pathSeparator;
		String path = System.getProperty("java.home") + sep + "lib" + sep
				+ "rt.jar";
		path += pathSep + "." + sep + "bin";
		Options.v().set_soot_classpath(path);
		
		// 载入MyClass类
		SootClass tgtClass = Scene.v().loadClassAndSupport(args[0]);
		// 把它作为我们要分析的类
		tgtClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		// 找到它的myMethod函数
		SootMethod method = tgtClass.getMethodByName("testInitVar");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		InitializedVarAnalysis.InitVarAnalysis an = new InitializedVarAnalysis.InitVarAnalysis(
				cfg);
		// iterate over the results
		for (Unit unit : cfg) {
			System.out.println(unit);
			FlowSet in = (FlowSet) an.getFlowBefore(unit);
			FlowSet out = (FlowSet) an.getFlowAfter(unit);
		}
	}

}
