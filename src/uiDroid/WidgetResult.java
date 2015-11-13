/*
 * precise CG
 */
package uiDroid;

import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class WidgetResult {
	public SootMethod eventHandler;
	public AbstractResource widget;
	public SootMethod sensitive;
	
	WidgetResult(SootMethod sensitive, SootMethod eventHandler, 
			AbstractResource widget) {
		this.sensitive = sensitive;
		this.eventHandler = eventHandler;
		this.widget = widget;
	}

}
