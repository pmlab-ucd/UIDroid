/*	
 * Store info for UI widget
 */

package uiDroid;

public class Widget {
	private String sid, text;
	private int type;
	
	Widget() {
		sid = null;
		type = -1;
		text = null;
	}
	
	public void setSid(String sid) {
		if (sid.contains("@id")) {
			this.sid = sid.split("@id/")[1];
		} else {
			this.sid = sid;
		}
	}
	
	public void setText(String text) {
		if (text.contains("@string")) {
			this.text = text.split("@string/")[1];
		} else {
			this.text = text;
		}
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

}
