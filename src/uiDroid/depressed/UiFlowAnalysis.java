/*
 * The interface of UiFlowAnalysis
 */
package uiDroid.depressed;

import java.util.List;

import soot.Local;
import soot.Unit;

public interface UiFlowAnalysis {
	public List<Local> getUILocalsBefore(Unit unit);
	public List<Local> getUILocalsAfter(Unit unit);
}
