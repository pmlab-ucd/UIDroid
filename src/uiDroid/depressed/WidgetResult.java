/*
 * precise CG
 */
package uiDroid.depressed;

import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class WidgetResult {
	public AbstractResource widget;
	public SootMethod sensitive, eventHandler, onCreate;
	
	WidgetResult(SootMethod sensitive, SootMethod onCreate, SootMethod eventHandler, 
			AbstractResource widget) {
		this.sensitive = sensitive;
		this.onCreate = onCreate;
		this.eventHandler = eventHandler;
		this.widget = widget;
	}

}
