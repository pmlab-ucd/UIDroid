package SootPlay;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;


public class MyVeryBusyExprAnalysisTest {
	
	/*
	 * 执行我们写的分析
	 */
	@Before
	public void setUp() throws Exception {
		// 载入MyClass类
		SootClass c = Scene.v().loadClassAndSupport("MyClass");
		// 把它作为我们要分析的类
		c.setApplicationClass();
		// 找到它的myMethod函数
		SootMethod m = c.getMethodByName("myMethod");
		// 获得它的函数体
		Body b = m.retrieveActiveBody();
		// 生成函数的cfg
		UnitGraph g = new ExceptionalUnitGraph(b);
		// 执行我们的分析
		MyVeryBusyExprAnalysis.VeryBusyExprAnalysis an = new MyVeryBusyExprAnalysis.VeryBusyExprAnalysis(g);
		// iterate over the results
		for (Unit unit: g) {
			FlowSet in = (FlowSet) an.getFlowBefore(unit);
			FlowSet out = (FlowSet) an.getFlowAfter(unit);
		}
				
	}

	@Test
	public void test() {
	}

}
