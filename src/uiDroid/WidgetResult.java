/*
 * precise CG
 */
package uiDroid;

import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class WidgetResult {
	public SootMethod eventHandler;
	public AbstractResource widget;
	
	WidgetResult(SootMethod eventHandler, 
			AbstractResource widget) {
		this.eventHandler = eventHandler;
		this.widget = widget;
	}

}
