package uiDroid;

import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class UiBackwardAnalysis extends BackwardFlowAnalysis<Object, Object>
		implements UiFlowAnalysis {
	private Stmt uiSetEventHandler = null; // e.g. setOnClick
	private List<Stmt> ids = new ArrayList<>();;

	@SuppressWarnings("unchecked")
	public UiBackwardAnalysis(DirectedGraph<?> graph) {
		super((DirectedGraph<Object>) graph);
		// doAnalysis();
	}

	public void run() {
		doAnalysis();
	}

	@Override
	protected void flowThrough(Object in, Object node, Object out) {
		// System.out.println("B: do");
		FlowSet inSet = (FlowSet) in, outSet = (FlowSet) out;
		Stmt stmt = (Stmt) node;
		copy(inSet, outSet);
		if (stmt == uiSetEventHandler) {
			// gen: source
			// should use == rather than equals
			System.out.println("B: found source!");
			InvokeExpr ie = stmt.getInvokeExpr();
			Value instance = ((InstanceInvokeExpr) ie).getBase();
			outSet.add(instance);
		} else if (stmt instanceof AssignStmt
				&& (stmt.toString().contains("findView")
						|| stmt.toString().contains("findItem"))) {
			// kill: sink
			if (inSet.contains(((AssignStmt) stmt).getLeftOp())) {
				System.out.println("B: found view id stmt");
				ids.add(stmt);
			}
			outSet.remove(((AssignStmt)stmt).getLeftOp());
		} else if (stmt instanceof AssignStmt
				&& inSet.contains(((AssignStmt) stmt).getLeftOp())) {
			outSet.remove(((AssignStmt) stmt).getLeftOp());
			if (stmt.containsInvokeExpr()) {
				InvokeExpr ie = stmt.getInvokeExpr();
				outSet.add(ie.getArg(0));
			} else {
				for (ValueBox box : ((AssignStmt) stmt).getRightOp()
						.getUseBoxes()) {
					outSet.add(box.getValue());
					System.out.println("B: " + box.getValue());
				}
			}
		}
	}

	public List<Stmt> getIdStmt() {
		return ids;
	}

	public void setEventHandler(Stmt setEventHandle) {
		uiSetEventHandler = setEventHandle;
	}

	@Override
	protected void copy(Object src, Object dest) {
		FlowSet srcSet = (FlowSet) src, destSet = (FlowSet) dest;
		srcSet.copy(destSet);
	}

	@Override
	protected Object entryInitialFlow() {
		return new ArraySparseSet();
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		FlowSet inSet1 = (FlowSet) in1, inSet2 = (FlowSet) in2, outSet = (FlowSet) out;
		inSet1.union(inSet2, outSet);
	}

	@Override
	protected Object newInitialFlow() {
		return new ArraySparseSet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Local> getUILocalsBefore(Unit unit) {
		return ((ArraySparseSet) getFlowBefore(unit)).toList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Local> getUILocalsAfter(Unit unit) {
		return ((ArraySparseSet) getFlowAfter(unit)).toList();
	}

}
