/*
 * Using FlowDroid to load class files of the apk
 */
package playGator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParserException;

//import app.MySetupApplication;

import com.google.common.collect.Lists;

import presto.android.Configs;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;

public class Config extends Configs{
	
	public static String apkPath = null;
	public static String platformDir = null;
	
	public static void processing() {
		File projectFile = new File(project);
		if (projectFile.isFile()) {
			apkModeProcessing();
			return;
		}
		
		bytecodes = "";//project + "/bin/classes";
		//System.out.println(bytecodes);
		
		//String sep = File.separator;
		//String pathSep = File.pathSeparator;
		//String classpath = System.getProperty("java.home") + sep + "lib" + sep
				//+ "rt.jar";
		platformDir = Config.sdkDir + "/platforms";// /android-17/android.jar";
		//classpath += pathSep + platformDir;
		String apkDir = apkPath;
		
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
		//String extraJar = "/home/hao/workspace/AppContext/libs";
		SetupApplication app = new SetupApplication(platformDir, apkDir);
		//MySetupApplication app = new MySetupApplication(platformDir, apkDir,
				//extraJar);
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
		Scene.v().addBasicClass("android.widget.TableLayout", SootClass.SIGNATURES);
		Scene.v().addBasicClass("android.widget.TableRow", SootClass.SIGNATURES);
		Scene.v().loadNecessaryClasses();

		// 创建dummy main并作为app的main函数(分析入口), 非GuiAnalysis所需
		/*SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
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
