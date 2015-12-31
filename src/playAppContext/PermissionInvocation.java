package playAppContext;

import java.util.ArrayList;
import java.util.List;
import soot.SootMethod;

public class PermissionInvocation {
	SootMethod src;
	SootMethod tgt;
	String permission;
	List<Context> contexts = new ArrayList<>();

	public PermissionInvocation(SootMethod src, SootMethod tgt,
			String permission) {
		this.src = src;
		this.tgt = tgt;
		this.permission = permission;
	}

	public PermissionInvocation(SootMethod src, SootMethod tgt) {
		this.src = src;
		this.tgt = tgt;
	}

	public List<Context> getContexts() {
		return contexts;
	}

	public void addContext(Context ctx) {
		contexts.add(ctx);
	}

	public void setContexts(List<Context> contexts) {
		this.contexts = contexts;
	}

	public SootMethod getSrc() {
		return src;
	}

	public void setSrc(SootMethod src) {
		this.src = src;
	}

	public SootMethod getTgt() {
		return tgt;
	}

	public void setTgt(SootMethod tgt) {
		this.tgt = tgt;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof PermissionInvocation))
			return false;
		PermissionInvocation invoc = (PermissionInvocation) obj;
		if ((getSrc().equals(invoc.getSrc()))
				&& (getTgt().equals(invoc.getTgt())))
			return true;
		return false;
	}
}
