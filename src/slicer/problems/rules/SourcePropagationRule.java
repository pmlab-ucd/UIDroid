package slicer.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import slicer.InfoflowManager;
import slicer.problems.TaintPropagationResults;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule to introduce unconditional taints at sources
 * 
 * @author Steven Arzt
 *
 */
public class SourcePropagationRule extends AbstractTaintPropagationRule {

	public SourcePropagationRule(InfoflowManager manager, Aliasing aliasing,
			Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, aliasing, zeroValue, results);
	}

	private Collection<Abstraction> propagate(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		if (source == getZeroValue()) {
			// Check whether this can be a source at all
			final SourceInfo sourceInfo = getManager().getSourceSinkManager() != null
					? getManager().getSourceSinkManager().getSourceInfo(stmt, getManager().getICFG()) : null;
					
			// We never propagate zero facts onwards
			killSource.value = true;
			
			// Is this a source?
			if (sourceInfo != null && !sourceInfo.getAccessPaths().isEmpty()) {
				Set<Abstraction> res = new HashSet<>();
				Value leftOp = stmt instanceof DefinitionStmt ? ((DefinitionStmt) stmt).getLeftOp() : null;
				for (AccessPath ap : sourceInfo.getAccessPaths()) {
					Abstraction abs = new Abstraction(ap,
							stmt,
							sourceInfo.getUserData(),
							false,
							false);
					res.add(abs);
					
					// Compute the aliases
					if (leftOp != null)
						if (Aliasing.canHaveAliases(stmt, leftOp, abs))
							getAliasing().computeAliases(d1, stmt, leftOp,
									res, getManager().getICFG().getMethodOf(stmt), abs);
					
					// Set the corresponding call site
					if (stmt.containsInvokeExpr())
						abs.setCorrespondingCallSite(stmt);
				}
				return res;
			}
			if (killAll != null)
				killAll.value = true;
		}
		return null;
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		return propagate(d1, source, stmt, killSource, killAll);
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		return propagate(d1, source, stmt, killSource, null);
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(
			Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1,
			Abstraction source, Stmt stmt, ByReferenceBoolean killAll) {
		// Normally, we don't inspect source methods
		if (!getManager().getConfig().getInspectSources()
				&& getManager().getSourceSinkManager() != null) {
			final SourceInfo sourceInfo = getManager().getSourceSinkManager().getSourceInfo(
					stmt, getManager().getICFG());
			if (sourceInfo != null)
				killAll.value = true;
		}
		
		// By default, we don't inspect sinks either
		if (!getManager().getConfig().getInspectSinks()
				&& getManager().getSourceSinkManager() != null) {
			final boolean isSink = getManager().getSourceSinkManager().isSink(
					stmt, getManager().getICFG(), source.getAccessPath());
			if (isSink)
				killAll.value = true;
		}
		
		return null;
	}

}
