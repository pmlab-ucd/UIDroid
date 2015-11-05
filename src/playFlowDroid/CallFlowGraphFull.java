/*
 * https://groups.google.com/forum/#!topic/soot-list/bob7WNBJwwU
 */

package playFlowDroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xmlpull.v1.XmlPullParserException;

import app.MyTest;
import app.PermissionInvocation;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.solver.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

public class CallFlowGraphFull extends MyTest{

	private static DotGraph dot = new DotGraph("callgraph");

	private static List<String> PscoutMethod;
	static String appName = "app-debug";
	private static String csvName;// = "output.csv";
	private static CallGraph cg;
	private static JimpleBasedInterproceduralCFG icfg;
	private static List<PermissionInvocation> perInvocs = new ArrayList<>();
	// control flow graph
	private static IInfoflowCFG flowcfg;
	// data flow analysis
	private static soot.jimple.infoflow.InfoflowResults flowResults;

	public CallFlowGraphFull() {
	}
	
	public static void setCsvName() {
		csvName = "output.csv";
	}
	
	public static void printCsvName() {
		System.out.print(csvName);
	}

	public static void main(String[] args) {
		// File f = new
		// File("/media/hao/Hitachi/PATDroid/PATDroidTest/app/app-debug.apk");
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
		
		
		soot.G.reset();

		SetupApplication app = new SetupApplication(
		// "C:/Users/hao/Downloads/android-sdk-windows/platforms",
				platformPath, apkPath);

		try {
			app.calculateSourcesSinksEntrypoints("./SourcesAndSinks.txt");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		
		// reset graph
		soot.G.reset();
		// load apk files
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Collections.singletonList(apkPath));
		Options.v().set_force_android_jar(
		// "C:/Users/hao/Downloads/android-sdk-windows/platforms");
				"/home/hao/Android/Sdk/platforms");
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		// no output or produces a Dex/APK file as output
		// Options.v().set_output_format(Options.output_format_none);
		Options.v().set_output_format(13);

		// Options.v().setPhaseOption("cg.spark", "on");
		Scene.v().loadNecessaryClasses();
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		// System.out.println(entryPoint.getActiveBody());
		// PackManager.v().runPacks();
		// System.out.println(Scene.v().getCallGraph().size());
		
		
		Transform CGtransform = new Transform("wjtp.checkCG",
				new SceneTransformer() {
					@Override
					protected void internalTransform(String phaseName,
							Map<String, String> options) {
						cg = Scene.v().getCallGraph();
						icfg = new JimpleBasedInterproceduralCFG();
						analyzeCG();
					}
				});

		PackManager.v().getPack("wjtp").add(CGtransform);
		PackManager.v().runPacks();

		// CallGraph cg = Scene.v().getCallGraph();
		// System.out.println("+++++++++++++++++" + cg);
		// System.out.println(".................."+entryPoint);
		// String label = Scene.v().getCallGraph().listener().toString();
		// dot.createSubGraph(label);
		// analyzeCG(cg);
		String dest = f.getName();
		String fileNameWithOutExt = FilenameUtils.removeExtension(dest);
		String destination = "./sootOutput/" + fileNameWithOutExt;
		dot.plot(destination + dot.DOT_EXTENSION);
	}

	/*
	 * travel over Call Graph by visit edges one by one
	 */
	private static void analyzeCG(CallGraph cg) {
		QueueReader<Edge> edges = cg.listener();
		Set<String> visited = new HashSet<>();

		File resultFile = new File("./sootOutput/CG.log");
		PrintWriter out = null;
		try {
			out = new PrintWriter(resultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		out.println("CG==================");
		// iterate over edges of call graph
		while (edges.hasNext()) {
			Edge edge = (Edge) edges.next();
			SootMethod target = (SootMethod) edge.getTgt();
			MethodOrMethodContext src = edge.getSrc();
			if (!visited.contains(src.toString())) {
				dot.drawNode(src.toString());
				visited.add(src.toString());
			}
			if (!visited.contains(target.toString())) {
				dot.drawNode(target.toString());
				visited.add(target.toString());
			}
			out.println(src + "  -->   " + target);
			dot.drawEdge(src.toString(), target.toString());

			// 如果是敏感函数
			if (PscoutMethod.contains(target.toString())) {
				System.out.println(target.toString());
				PermissionInvocation perInvoc = new PermissionInvocation(
						(SootMethod) src, target);
				if (!perInvocs.contains(perInvoc)) {
					System.out.println("begin per analysis: " + new Date());
					// 获得敏感函数所对应的权限
					perInvoc.setPermission(MyTest.getPermissionForInvoc(
							target.toString(), PscoutMethod));
					MyTest.printCFGpath((SootMethod) src, target, icfg, cg, perInvoc);
					System.out.println("end per analysis: " + new Date());
					perInvocs.add(perInvoc);
					MyTest.analyzeFlowResult(perInvoc);
				}
			}
		}

		out.println("CG ends==================");
		out.close();
		System.out.println(cg.size());
	}
}