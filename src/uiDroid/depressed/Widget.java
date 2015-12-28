/*	
 * Store info for UI widget
 */

package uiDroid.depressed;

import java.util.ArrayList;
import java.util.List;

public class Widget {
	private String sid, text;
	private List<String> callbacks;
	private int type;
	
	Widget() {
		sid = null;
		type = -1;
		text = null;
		callbacks = new ArrayList<>();
	}
	
	public void setSid(String sid) {
		if (sid.contains("@id")) {
			this.sid = sid.split("@id/")[1];
		} else {
			this.sid = sid;
		}
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public String getText() {
		return text;
	}
	
	public int getType() {
		return type;
	}
	
	public String getSid() {
		return sid;
	}
	
	public void setCallback(String callback) {
		callbacks.add(callback); 
	}
	
	public List<String> getCallback() {
		return callbacks;
	}

}
