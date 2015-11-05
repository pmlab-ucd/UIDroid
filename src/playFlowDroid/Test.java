package playFlowDroid;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import app.MyTest;
import app.PermissionInvocation;

public class Test extends MyTest {
	private static List<String> PscoutMethod;
	private static List<PermissionInvocation> perInvocs;
	private static JimpleBasedInterproceduralCFG icfg;

	public static void main(String[] args) {
		File f = new File(
				"/home/hao/workspace/AppContext/Instrument/InstrumentedApp/ApkSamples/app-debug.apk");
		String apkPath = f.getAbsolutePath();
		String platformPath = "/home/hao/Android/Sdk/platforms";
		
		try {
			PscoutMethod = FileUtils.readLines(new File(
					"./jellybean_publishedapimapping_parsed.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		permissionAnalysis(apkPath, platformPath, "/home/hao/workspace/AppContext/libs");
	}
}
