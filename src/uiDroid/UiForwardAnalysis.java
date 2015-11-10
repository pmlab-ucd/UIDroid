package uiDroid;

import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class UiForwardAnalysis {
	public static void main(String[] args) {
		PackManager.v().getPack("jtp")
				.add(new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body body, String phase,
							Map options) {
						new UiForwardVarAnalysis(new ExceptionalUnitGraph(body));
						// use G.v().out instead of System.out so that Soot can
						// redirect this output to the Eclipse console
						G.v().out.println(body.getMethod());
					}

				}));

		soot.Main.main(args);
	}
	
	/*
	 * perform an init var analysis using Soot. 
	 * nature: forward, may
	 * lattice element: the set of possibly uninit vars 
	 */
	public static class UiForwardVarAnalysis extends
			ForwardFlowAnalysis<Object, Object> {
		
		@SuppressWarnings("unchecked")
		public UiForwardVarAnalysis(DirectedGraph<?> exceptionalUnitGraph) {
			// use superclass's constructor
			super((DirectedGraph<Object>) exceptionalUnitGraph);
			doAnalysis();
		}

		@Override
		protected void flowThrough(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in, outSet = (FlowSet)out;
			Unit unit = (Unit) node;
			if (!gen(inSet, unit, outSet)) {
				kill(unit, outSet);
			}
		}
		
		/*
		 * rm tainted var who has been assigned value from un-tainted
		 */
		private void kill(Object node, Object out) {
			FlowSet	outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			if (unit instanceof AssignStmt) {
				for (ValueBox defBox : unit.getDefBoxes()) {
					Value value = defBox.getValue();
					if (value instanceof Local && outSet.contains(value)) {
						System.out.println("Kill here! " + unit);
						outSet.remove(value);
					}
				}
			}
		}
		
		/*
		 * add vars possibly instanced by Handler 
		 */
		private boolean gen(Object in, Object node, Object out) {
			FlowSet inSet = (FlowSet)in,
					outSet = (FlowSet)out;
			Unit unit = (Unit)node;
			copy(inSet, outSet);
			boolean hasTainted = false;
			if (unit instanceof AssignStmt) {
				// if x = new Activity1$1 (a event handler class)
				//if (((AssignStmt) unit).containsInvokeExpr()
				if (unit.toString().contains("new")
						&& unit.toString().contains("Activity1$")) {
					System.out.println("found Source! " + unit);
					addDefBox(unit, outSet);
					hasTainted = true;
				}
				// if x := y, where y is tainted
				for (ValueBox useBox : unit.getUseBoxes()) {
					Value useVal = useBox.getValue();
					if (inSet.contains(useVal)) {
						addDefBox(unit, outSet);
						hasTainted = true;
						break;
					}
				}
			} else {
				// if x.setOnClickLinster(y)
				if (((Stmt) unit).containsInvokeExpr()
						&& unit.toString().contains("setOnClickListener")) {
					InvokeExpr ie = ((Stmt)unit).getInvokeExpr();
					System.out.println("found onClick! " + unit);
					for (Value arg : ie.getArgs()) {
						if (inSet.contains(arg)) {
							if (ie instanceof InstanceInvokeExpr) {
								Value instance = ((InstanceInvokeExpr)ie).getBase();
								outSet.add(instance);
							}
						}
					}
				}
			}
			
			return hasTainted;
		}
		
		private void addDefBox(Unit unit, FlowSet outSet) {
			for (ValueBox defBox : unit.getDefBoxes()) {
				Value value = defBox.getValue();
				if (value instanceof Local) {
					outSet.add(value);
				}
			}
		}

		@Override
		protected void copy(Object src, Object dest) {
			FlowSet srcSet = (FlowSet)src,
					destSet = (FlowSet)dest;
			srcSet.copy(destSet);
		}

		@Override
		protected Object entryInitialFlow() {
			return new ArraySparseSet();
		}

		@Override
		protected void merge(Object in1, Object in2, Object out) {
			FlowSet inSet1 = (FlowSet)in1, 
					inSet2 = (FlowSet)in2,
					outSet = (FlowSet)out;
			inSet1.union(inSet2, outSet);
		}

		@Override
		protected Object newInitialFlow() {
			return new ArraySparseSet();
		}
		
		@SuppressWarnings("unchecked")
		public List<Local> getLiveLocalsAfter(Unit s) {
			// ArraySparseSet returns a unbacked list of elements!
			return ((ArraySparseSet) getFlowAfter(s)).toList();
		}

		@SuppressWarnings("unchecked")
		public List<Local> getLiveLocalsBefore(Unit s) {
			// ArraySparseSet returns a unbacked list of elements!
			return ((ArraySparseSet) getFlowBefore(s)).toList();
		}
	}

}
