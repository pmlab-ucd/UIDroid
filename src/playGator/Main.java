/*
 * reference: presto.android.Main
 */
package playGator;

import java.io.File;
import java.util.Map;

import presto.android.Configs;
import presto.android.Debug;
import soot.Pack;
import soot.PackManager;
import soot.SceneTransformer;
import soot.Transform;
//import soot.options.Options;

public class Main {
	public static void main(String[] args) {
		Debug.v().setStartTime();
		parseArgs(args);
		System.out.println("finish parsing args!");
		presto.android.Main.checkAndPrintEnvironmentInformation(args);
		System.out.println("finish check and print print env info!");
		setupAndInvokeSoot();
	}

	// TODO more args
	public static void parseArgs(String[] args) {
		String pathSep = File.pathSeparator;
		args = new String[] {
				// resource(xml) folder
				"-project", "/home/hao/workspace/ApkSamples/Decompiled/app-debug",
				"-sdk", "/home/hao/Android/Sdk",
				"-android", "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/framework.jar"
						+ pathSep + "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/bouncycastle.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/ext.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/android-policy.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/services.jar:"
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/annotations.jar:"
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/android-support-v4.jar:"
						+ "/home/hao/Android/Sdk/platforms/android-17/android.jar",
				"-apiLevel", "android-17",
				"-jre", "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/core.jar",
				"-benchmarkName", "Button1",
				"-listenerSpecFile", "/home/hao/workspace/gator-3.0/SootAndroid/listeners.xml",
				"-wtgSpecFile", "/home/hao/workspace/gator-3.0/SootAndroid/wtg.xml",
				"-guiAnalysis",
		};
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-project".equals(arg)) {
				// the target project
				Config.project = args[++i];
			} else if ("-sdk".equals(arg)) {
				Config.sdkDir = args[++i];
			} else if ("-guiAnalysis".equals(arg)) {
				Config.guiAnalysis = true;
			} else if ("-android".equals(arg)) {
				// To read in platformJar
				Config.android = args[++i];
				//Options.v().set_soot_classpath(Configs.android);
			} else if ("-verbose".equals(arg)) {
				Config.verbose = true;
			} else if ("-apiLevel".equals(arg)) {
				Config.apiLevel = args[++i];
			} else if ("-jre".equals(arg)) {
				Config.jre = args[++i];
			} else if ("-benchmarkName".equals(arg)) {
				// The app name.
				Config.benchmarkName = args[++i];
			} else if ("-listenerSpecFile".equals(arg)) {
				Config.listenerSpecFile = args[++i];
			} else if ("-wtgSpecFile".equals(arg)) {
				Config.wtgSpecFile = args[++i];
			} else {
				throw new RuntimeException("Unknow option: " + arg);
			} 
		}
		
		Config.processing();
	}

	public static void setupAndInvokeSoot() {
		String classpath = presto.android.Main.computeClasspath();
		String packName = "wjtp";
		String phaseName = "wjtp.gui";
		String[] sootArgs = { "-w", "-p", "cg", "all-reachable:true", "-p",
				"cg.cha", "enabled:true", "-p", phaseName, "enabled:true",
				"-f", "n", "-keep-line-number", "-allow-phantom-refs",
				"-process-dir", Configs.bytecodes, "-cp", classpath};
		invokeSoot(packName, phaseName, sootArgs);
	}

	public static void invokeSoot(String packName, String phaseName,
			String[] sootArgs) {
		// create the phase and add it to the pack
		Pack pack = PackManager.v().getPack(packName);

		pack.add(new Transform(phaseName, new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName,
					Map<String, String> options) {
				EntryPointAnalysis.v().run();
			}
		}));
		// invoke Soot
		soot.Main.main(sootArgs);
	}
}
