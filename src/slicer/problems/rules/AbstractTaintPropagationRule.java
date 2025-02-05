package slicer.problems.rules;

import slicer.InfoflowManager;
import slicer.problems.TaintPropagationResults;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;

/**
 * Abstract base class for all taint propagation rules
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractTaintPropagationRule implements
		ITaintPropagationRule {
	
	private final InfoflowManager manager;
	private final Aliasing aliasing;
	private final Abstraction zeroValue;
	private final TaintPropagationResults results;
	
	public AbstractTaintPropagationRule(InfoflowManager manager,
			Aliasing aliasing, Abstraction zeroValue,
			TaintPropagationResults results) {
		this.manager = manager;
		this.aliasing = aliasing;
		this.zeroValue = zeroValue;
		this.results = results;
	}
	
	protected InfoflowManager getManager() {
		return this.manager;
	}
	
	protected Aliasing getAliasing() {
		return this.aliasing;
	}
	
	protected Abstraction getZeroValue() {
		return this.zeroValue;
	}
	
	protected TaintPropagationResults getResults() {
		return this.results;
	}
	
}
