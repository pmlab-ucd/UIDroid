package slicer.problems.rules;

import java.util.Collection;

import slicer.InfoflowManager;
import slicer.problems.TaintPropagationResults;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for recording abstractions that arrive at sinks
 * 
 * @author Steven Arzt
 */
public class SinkPropagationRule extends AbstractTaintPropagationRule {

	public SinkPropagationRule(InfoflowManager manager, Aliasing aliasing,
			Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, aliasing, zeroValue, results);
	}
	
	public boolean checkSrc(Abstraction source) {
		if (source.getSourceContext() != null) {
			//System.out.println("Hao src: " + source.getSourceContext().getStmt());
			//System.out.println("" + getManager().getSourceSinkManager().
					//getSourceInfo(source.getSourceContext().getStmt(), getManager().getICFG()));
			if (source.getSourceContext().getStmt().containsInvokeExpr()) {
				SootMethod method = source.getSourceContext().getStmt().getInvokeExpr().getMethod();
				//System.out.println("Hao src: " + source.getSourceContext().getStmt().getInvokeExpr().getMethod());
				for (SourceSinkDefinition am : getManager().getSourceSinkProvider().getSources()) {
					if (am.toString().equals(method.getSignature())) {
						//System.out.println("HEREH " + am.toString());
						return true;
					}
				}			
			}		
			System.out.println("Hao src: " + source.getSourceContext().getStmt());
		}
	
		return false;
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			checkForSink(d1, source, stmt, returnStmt.getOp());
		}
		else if (stmt instanceof IfStmt) {
			final IfStmt ifStmt = (IfStmt) stmt;
			checkForSink(d1, source, stmt, ifStmt.getCondition());
		}
		else if (stmt instanceof LookupSwitchStmt) {
			final LookupSwitchStmt switchStmt = (LookupSwitchStmt) stmt;
			checkForSink(d1, source, stmt, switchStmt.getKey());
		}
		else if (stmt instanceof TableSwitchStmt) {
			final TableSwitchStmt switchStmt = (TableSwitchStmt) stmt;
			checkForSink(d1, source, stmt, switchStmt.getKey());
		}
		else if (stmt instanceof AssignStmt) {
			final AssignStmt assignStmt = (AssignStmt) stmt;
			checkForSink(d1, source, stmt, assignStmt.getRightOp());
		}
		
		return null;
	}
	
	/**
	 * Checks whether the given taint abstraction at the given statement triggers
	 * a sink. If so, a new result is recorded
	 * @param d1 The context abstraction
	 * @param source The abstraction that has reached the given statement
	 * @param stmt The statement that was reached
	 * @param retVal The value to check
	 */
	private void checkForSink(Abstraction d1, Abstraction source, Stmt stmt,
			final Value retVal) {
		// The incoming value may be a complex expression. We have to look at
		// every simple value contained within it.
		for (Value val : BaseSelector.selectBaseList(retVal, false)) {
			if (getManager().getSourceSinkManager() != null
					 && source.isAbstractionActive()
					 && getAliasing().mayAlias(val, source
							.getAccessPath().getPlainValue())){
					//&& getManager().getSourceSinkManager().isSink(stmt,
					//		getManager().getICFG(), source.getAccessPath())) {
					 //&& checkSrc(source)){
				getResults().addResult(new AbstractionAtSink(source, stmt));
			}
			//getResults().addResult(new AbstractionAtSink(source, stmt));
		}
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		// The given access path must at least be referenced somewhere in the
		// sink
		if (source.isAbstractionActive()
				&& !source.getAccessPath().isStaticFieldRef()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			boolean found = false;
			for (int i = 0; i < iexpr.getArgCount(); i++)
				if (getAliasing().mayAlias(iexpr.getArg(i),
						source.getAccessPath().getPlainValue())) {
					if (source.getAccessPath().getTaintSubFields()
							|| source.getAccessPath().isLocal()) {
						found = true;
						break;
					}
				}
			if (!found && iexpr instanceof InstanceInvokeExpr)
				if (((InstanceInvokeExpr) iexpr).getBase() == source
						.getAccessPath().getPlainValue())
					found = true;

			// Is this a call to a sink?
			if (found
					&& getManager().getSourceSinkManager() != null){
					//&& getManager().getSourceSinkManager().isSink(stmt,
							//getManager().getICFG(), source.getAccessPath())) {
					//&& checkSrc(source)){
				// Hao: 
				getResults().addResult(new AbstractionAtSink(source, stmt));
			}
			//Hao: add here
			//getResults().addResult(new AbstractionAtSink(source, stmt));
		}

		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(
			Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		// Check whether this return is treated as a sink
		if (stmt instanceof ReturnStmt) {
			final ReturnStmt returnStmt = (ReturnStmt) stmt;
			boolean matches = source.getAccessPath().isLocal()
					|| source.getAccessPath().getTaintSubFields();
			if (matches
					&& source.isAbstractionActive()
					&& getManager().getSourceSinkManager() != null
					&& getAliasing().mayAlias(source.getAccessPath().getPlainValue(),
							returnStmt.getOp())){
					//&& getManager().getSourceSinkManager().isSink(returnStmt,
							//getManager().getICFG(), source.getAccessPath())) {
					 //&& checkSrc(source)){
				getResults().addResult(new AbstractionAtSink(source, returnStmt));
			}
			//getResults().addResult(new AbstractionAtSink(source, returnStmt));
		}
		return null;
	}

}