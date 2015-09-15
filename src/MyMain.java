import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class MyMain {
	public static void main(String[] args) {
		// 载入MyClass类
		SootClass c = Scene.v().loadClassAndSupport("MyClass");
		// 把它作为我们要分析的类
		c.setApplicationClass();
		// 找到它的myMethod函数
		SootMethod method = c.getMethodByName("myMethod");
		// 获得它的函数体
		Body body = method.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		MyVeryBusyExprAnalysis.VeryBusyExprAnalysis an = new MyVeryBusyExprAnalysis.VeryBusyExprAnalysis(
				cfg);
		// iterate over the results
		for (Unit unit : cfg) {
			FlowSet in = (FlowSet) an.getFlowBefore(unit);
			FlowSet out = (FlowSet) an.getFlowAfter(unit);
			System.out.println(unit.toString() + ": " + in.toString() + ' ' + out.toString());
		}
	}
}
