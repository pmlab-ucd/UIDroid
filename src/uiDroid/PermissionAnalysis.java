/*
 * Sensitive Permission Analysis
 * --- Copyright Hao Fu 2015. All rights reserved. -----------------
 */

package uiDroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xmlpull.v1.XmlPullParserException;

import playGator.Config;
import presto.android.Configs;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

public class PermissionAnalysis {
	private static CallGraph cg;
	// sensitive permission related
	private static List<String> PscoutMethod;
	public static Map<SootMethod, List<SootMethod>> sensEntries = new HashMap<>();
	private static DotGraph dot = new DotGraph("callgraph");
	private static String apkPath = Configs.project;

	// The nested class to implement singleton
	private static class SingletonHolder {
		private static final PermissionAnalysis instance = new PermissionAnalysis();
	}

	// Get THE instance
	public static final PermissionAnalysis v() {
		return SingletonHolder.instance;
	}

	/*
	 * get all onCreate()
	 */
	public static List<SootMethod> getAllOnCreate() {
		QueueReader<Edge> edges = cg.listener();
		List<SootMethod> res = new ArrayList<>();
		while (edges.hasNext()) {
			Edge edge = (Edge) edges.next();
			SootMethod target = (SootMethod) edge.getTgt();

			String tgtMethod = target.toString();

			// onCreate()
			if (tgtMethod.contains("onCreate")) {
				res.add(target);
			}
		}

		return res;
	}

	/*
	 * Traverse over Call Graph by visit edges one by one check whether is a
	 * sensitive permission related API call. if is, get the entry
	 */
	@SuppressWarnings("static-access")
	public static void getEntries() {
		QueueReader<Edge> edges = cg.listener();
		Set<String> visited = new HashSet<>();

		File resultFile = new File("./sootOutput/CGTest.log");
		PrintWriter out = null;
		try {
			out = new PrintWriter(resultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		out.println("CG begins==================");
		// iterate over edges of call graph
		while (edges.hasNext()) {
			Edge edge = (Edge) edges.next();
			SootMethod target = (SootMethod) edge.getTgt();
			SootMethod src = edge.getSrc().method();
			if (!visited.contains(src.toString())) {
				dot.drawNode(src.toString());
				visited.add(src.toString());
			}
			if (!visited.contains(target.toString())) {
				dot.drawNode(target.toString());
				visited.add(target.toString());
			}
			dot.drawEdge(src.toString(), target.toString());
			out.println(src + "  -->   " + target);
			if (PscoutMethod.contains(target.toString())) {
				bfsCG(target);
			}
		}

		out.println("CG ends==================");
		out.close();
		System.out.println(cg.size());
		String fileNameWithOutExt = FilenameUtils.getName(apkPath);
		fileNameWithOutExt = FilenameUtils.removeExtension(fileNameWithOutExt);
		String destination = "./sootOutput/" + fileNameWithOutExt;
		String dotPath = destination + dot.DOT_EXTENSION;
		dot.plot(dotPath);
	}

	/*
	 * bfs the CG to get the entries of all sensitive methods
	 */
	public static void bfsCG(SootMethod target) {
		Queue<SootMethod> queue = new LinkedList<>();
		Set<SootMethod> visited = new HashSet<>();
		queue.add(target);
		while (!queue.isEmpty()) {
			int len = queue.size();
			for (int i = 0; i < len; i++) {
				SootMethod node = queue.poll();
				if (visited.contains(node)) {
					continue;
				}
				visited.add(node);
				Iterator<Edge> iterator = cg.edgesInto(node);
				while (iterator.hasNext()) {
					Edge in = iterator.next();
					if (in.getSrc().method().toString().contains("dummy")) {
						if (!sensEntries.containsKey(target)) {
							sensEntries
									.put(target, new ArrayList<SootMethod>());
						}
						List<SootMethod> mList = sensEntries.get(target);
						mList.add(in.getTgt().method());
						System.out
								.println(target + ": " + in.getTgt().method());
					}
					queue.add(in.getSrc().method());
				}
			}
		}
	}

	public void run() {

		SetupApplication app = new SetupApplication(Config.platformDir, Config.apkPath);
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
		Options.v().set_process_dir(Collections.singletonList(Config.apkPath));
		Options.v().set_android_jars(Config.platformDir);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(13);
		// This statement is necessary to make XMLParser in gator to run correcly
		Scene.v().addBasicClass("android.widget.RelativeLayout", SootClass.SIGNATURES);
		Scene.v().loadNecessaryClasses();

		// 创建dummy main并作为app的main函数(分析入口), 非GuiAnalysis所需
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		
		try {
			PscoutMethod = FileUtils.readLines(new File(
					"./jellybean_publishedapimapping_parsed.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Transform CGtransform = new Transform("wjtp.checkCG",
				new SceneTransformer() {
					@Override
					protected void internalTransform(String phaseName,
							Map<String, String> options) {
						cg = Scene.v().getCallGraph();
						// icfg = new JimpleBasedInterproceduralCFG();
						getEntries();
					}
				});

		PackManager.v().getPack("wjtp").add(CGtransform);
		try {
			System.out
					.println("RUN PACKSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS!");
			PackManager.v().runPacks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
