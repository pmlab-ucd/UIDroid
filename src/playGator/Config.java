package playGator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParserException;

import com.google.common.collect.Lists;

import presto.android.Configs;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;

public class Config extends Configs{
	public static void processing() {
		File projectFile = new File(project);
		if (projectFile.isFile()) {
			apkModeProcessing();
			return;
		}
		
		bytecodes = project + "/bin/classes";
		System.out.println(bytecodes);
		
		//String sep = File.separator;
		//String pathSep = File.pathSeparator;
		//String classpath = System.getProperty("java.home") + sep + "lib" + sep
				//+ "rt.jar";
		String platformDir = "/home/hao/Android/Sdk/platforms";// /android-17/android.jar";
		//classpath += pathSep + platformDir;
		String apkDir = "/home/hao/workspace/ApkSamples/app-debug.apk";
		
		//Options.v().set_soot_classpath(classpath);
		//Scene.v().loadNecessaryClasses();
		
		//Scene.v().loadClassAndSupport("de.ecspride.Activity1");
		//Scene.v().loadClassAndSupport("de.ecspride.Activity2");
	      /*String[] sootArgs = {
	              "-w",
	              "-p", "cg", "all-reachable:true",
	              "-p", "cg.cha", "enabled:true",
	             // "-p", phaseName, "enabled:true",
	              "-f", "n",
	              "-keep-line-number",
	              "-allow-phantom-refs",
	              "-process-dir", Configs.bytecodes,
	              "-cp", classpath,
	          };
	      soot.Main.main(sootArgs);*/
		SetupApplication app = new SetupApplication(platformDir, apkDir);
		try {
			app.calculateSourcesSinksEntrypoints("./SourcesAndSinks.txt");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		// setup
		G.reset();
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Collections.singletonList(apkDir));
		Options.v().set_android_jars(platformDir);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(13);
		// This statement is necessary to make XMLParser in gator to run correcly
		Scene.v().addBasicClass("android.widget.RelativeLayout", SootClass.SIGNATURES);
		Scene.v().loadNecessaryClasses();

		// 创建dummy main并作为app的main函数(分析入口)
/*		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));*/
		
	    numericApiLevel = Integer.parseInt(apiLevel.substring("android-".length()));
	    sysProj = Configs.sdkDir + "/platforms/" + Configs.apiLevel + "/data";
	    // make validate() happy
	    depJars = Lists.newArrayList();
	    extLibs = Lists.newArrayList();


	    validate();
	    System.out.println("[Config] finished.");
	}
}
