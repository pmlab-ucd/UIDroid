package playAppContext;

import java.util.ArrayList;
import java.util.List;

import soot.SootMethod;
import soot.jimple.Ref;
import soot.jimple.Stmt;

/**
 * @ClassName: Context
 * @Description: The context of the sensitive API call
 * @author: Hao Fu
 * @date: Dec 29, 2015 6:54:37 PM
 */
public class Context {
	/**
	 * @Fields conditionalStmt : pred conditional stmts who influence the exec
	 *         of the method
	 */
	List<Stmt> conditionalStmt;
	List<SootMethod> factorMethod;
	/**
	 * @Fields entrypoint : The entry point of the API call at the cg
	 */
	SootMethod entrypoint;
	List<Ref> factorRef;
	
	List<Stmt> otherFactor;

	public SootMethod getEntrypoint() {
		return entrypoint;
	}

	public void setEntrypoint(SootMethod entrypoint) {
		this.entrypoint = entrypoint;
	}

	public List<Stmt> getConditionalStmt() {
		return conditionalStmt;
	}

	public void setConditionalStmt(List<Stmt> conditionalStmt) {
		this.conditionalStmt = conditionalStmt;
	}

	public void addFactorMethod(SootMethod v) {
		if (factorMethod == null)
			factorMethod = new ArrayList<>();
		factorMethod.add(v);
	}

	public boolean hasFactorMethod(SootMethod v) {
		if (factorMethod == null)
			factorMethod = new ArrayList<>();
		return factorMethod.contains(v);
	}

	public List<SootMethod> getFactorMethod() {
		return factorMethod;
	}

	public void setFactorMethod(List<SootMethod> factorMethod) {
		this.factorMethod = factorMethod;
	}

	public List<Ref> getFactorRef() {
		return factorRef;
	}

	public void setFactorRef(List<Ref> factorRef) {
		this.factorRef = factorRef;
	}

	public void addFactorRef(Ref r) {
		if (factorRef == null) {
			factorRef = new ArrayList<>();
		}
		factorRef.add(r);
	}

	public boolean hasFactorRef(Ref r) {
		if (factorRef == null)
			factorRef = new ArrayList<>();
		return factorRef.contains(r);
	}
	
	public void addOtherFactor(Stmt r) {
		if (otherFactor == null) {
			otherFactor = new ArrayList<>();
		}
		otherFactor.add(r);
	}
	
	public List<Stmt> getOtherFactor() {
		return otherFactor;
	}
	
	public boolean hasOtherFactor(Stmt factorValue) {
		if (otherFactor == null)
			otherFactor = new ArrayList<>();
		return otherFactor.contains(factorValue);
	}
}
