/*
 * Result class to store analysis result
 */
package uiDroid;

import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class CSVResult {
	private SootMethod senstive;
	private AbstractResource widget;
	
	CSVResult(SootMethod senstive, AbstractResource widget) {
		this.senstive = senstive;
		this.widget = widget;
	}
}
