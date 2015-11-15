package uiDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import soot.Body;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
//import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;
import uiDroid.UiForwardAnalysis.UiForwardVarAnalysis;
import app.MySetupApplication;
import app.MyTest;

public class UiDroidTest extends MyTest {
	// basics for analysis
	private static String apkPath;
	private static CallGraph cg;
	// private static JimpleBasedInterproceduralCFG icfg;
	private static ARSCFileParser fileParser = new ARSCFileParser();

	// sensitive permission related
	private static List<String> PscoutMethod;
	private static Map<SootMethod, List<SootMethod>> sensEntries = new HashMap<>();
	private static List<WidgetResult> widgetResult;
	private static DotGraph dot = new DotGraph("callgraph");
	private static String dotPath = null;
	private static List<String> eventHandlerTemps;// = new ArrayList<>();
	private static List<SootMethod> allOnCreate;

	public static void main(String[] args) {
		try {
			myTestMain(args);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void myTestMain(String[] args) throws IOException,
			InterruptedException {
		// 读入文件并判断是否是apk文件
		List<String> apkFiles = new ArrayList<>();
		File apkFile = new File(args[0]);
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}
			});
			for (String s : dirFiles) {
				apkFiles.add(s);
			}
		} else {
			// 获得文件类型
			String extension = apkFile.getName().substring(
					apkFile.getName().lastIndexOf("."));
			if (extension.equalsIgnoreCase(".txt")) {
				BufferedReader rdr = new BufferedReader(new FileReader(apkFile));
				String line = null;
				while ((line = rdr.readLine()) != null)
					apkFiles.add(line);
				rdr.close();
			} else if (extension.equalsIgnoreCase(".apk"))
				apkFiles.add(args[0]);
			else {
				System.err.println("Invalid input file format: " + extension);
				return;
			}
		}

		// 读入Pscout
		PscoutMethod = FileUtils.readLines(new File(
				"./jellybean_publishedapimapping_parsed.txt"));
		// 读入所有可能的event handlers
		eventHandlerTemps = FileUtils
				.readLines(new File("./EventHandlerTemps"));
		// System.out.println(eventHandlerTemps);
		String platformPath = "/home/hao/Android/Sdk/platforms";
		String extraJar = "/home/hao/workspace/AppContext/libs";

		for (final String fileName : apkFiles) {
			// File file = new File(
			// "/home/hao/workspace/ApkSamples/app-debug.apk");
			// apkPath = file.getAbsolutePath();
			apkPath = args[0] + File.separator + fileName;
			fileParser.parse(apkPath);
			widgetResult = new ArrayList<>();

			permissionAnalysis(apkPath, platformPath, extraJar);

			String decomPath = args[0] + File.separator + "Decompiled"
					+ File.separator + fileName;
			decompile(decomPath);
			HandleResult.storeResult(dotPath, widgetResult, decomPath);
		}
	}

	/*
	 * run apktool to decompiled the apk
	 */
	public static void decompile(String decomPath) throws IOException,
			InterruptedException {
		String cmd = "apktool d " + apkPath + " -o " + decomPath;
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);
		pr.waitFor();
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				pr.getInputStream()));
		String line = "";
		while ((line = buf.readLine()) != null) {
			System.out.println(line);
		}
	}

	/*
	 * traverse over Call Graph by visit edges one by one check whether is a
	 * sensitive permission related API call. if is, get the entry
	 */
	@SuppressWarnings("static-access")
	public static void getEntries() {
		QueueReader<Edge> edges = cg.listener();
		allOnCreate = getAllOnCreate();
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
		dotPath = destination + dot.DOT_EXTENSION;
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

	/*
	 * get Ui widgets from UI event handlers
	 */
	public static void getWidgets() {
		for (SootMethod sensitive : sensEntries.keySet()) {
			for (SootMethod entry : sensEntries.get(sensitive)) {
				System.out.println("Start ++++++++" + entry.getName());
				if (eventHandlerTemps.contains(entry.getName())) {
					// iterate over edges of call graph,
					// check whether sensitive whether entry point exists in
					// onCreate()
					System.out.println(entry);
					int wsize = widgetResult.size();
					for (SootMethod onCreate : allOnCreate) {
						// String baseClass =
						// entry.getDeclaringClass().toString()
						// .split("\\$")[0];
						// if (onCreate.getDeclaringClass().toString()
						// .contains(baseClass)) {
						// get callees inside a method body through icfg
						
						analyzeOnCreate(onCreate, entry, sensitive);
					}
					if (widgetResult.size() == wsize) {
						widgetResult.add(new WidgetResult(sensitive, entry, entry,
								null));
					}
				} else {
					widgetResult.add(new WidgetResult(sensitive, entry, entry,
							null));
				}
			}
		}

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
	 * retrieve interested sensitive widgets by parsing onCreate methods
	 */
	public static void analyzeOnCreate(SootMethod onCreate,
			SootMethod eventHandler, SootMethod senstive) {
		Body body = onCreate.retrieveActiveBody();
		// 生成函数的control flow graph
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		boolean runAnalysis = false;
		for (Unit unit : cfg) {
			if (unit.toString().contains("new")
					&& unit.toString().contains(
							eventHandler.getDeclaringClass().toString())) {
				runAnalysis = true;
				break;
			}
		}

		if (!runAnalysis) {
			return;
		}

		// 执行我们的分析
		UiForwardVarAnalysis.uiEventHandler = eventHandler;
		UiForwardAnalysis.UiForwardVarAnalysis ta = new UiForwardAnalysis.UiForwardVarAnalysis(
				cfg);

		// iterate over the results
		for (Unit unit : cfg) {
			// System.out.println(unit);
			List<Local> after = ta.getUILocalsAfter(unit);
			
			if (!after.isEmpty()) {			
				UiBackwardAnalysis ba = new UiBackwardAnalysis(cfg);
				ba.setEventHandler((Stmt)unit);
				ba.run();
				//PrintFlowResult.print(cfg, body, ba);
				for (Stmt idStmt : ba.getIdStmt()) {
					int id = extractId((Stmt) idStmt);
					System.out.println(id);
					AbstractResource widget = fileParser
							.findResource(id);
					System.out.println(widget.getResourceName());
					WidgetResult widgetRes = new WidgetResult(senstive, onCreate,
							eventHandler, widget);
					widgetResult.add(widgetRes);
				}
				/*
				for (Local value : after) {
					Map<Value, Unit> widgetMap = bfsCFG(unit, value, cfg, 0,
							null);
					for (Value val : widgetMap.keySet()) {
						Map<Value, Unit> actionMap = bfsCFG(widgetMap.get(val),
								null, cfg, 2, val);
						if (!actionMap.isEmpty()) {
							widgetMap = actionMap;
							break;
						}
					}
					for (Value val : widgetMap.keySet()) {
						Map<Value, Unit> idMap = bfsCFG(widgetMap.get(val),
								null, cfg, 1, val);
						for (Unit stmt : idMap.values()) {
							int id = extractId((Stmt) stmt);
							System.out.println(id);
							AbstractResource widget = fileParser
									.findResource(id);
							System.out.println(widget.getResourceName());
							widgetRes = new WidgetResult(senstive, onCreate,
									eventHandler, widget);
							widgetResult.add(widgetRes);
						}
					}
				}*/
			}
		}
	}

	/*
	 * Extract id from stmt, e.g. findViewById(id)
	 */
	public static int extractId(Stmt stmt) {
		if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			for (Value arg : ie.getArgs()) {
				System.out.println(arg.getType().getNumber());
				if (arg.getType().getNumber() == 16) {
					return Integer.parseInt(arg.toString());
				}
			}
		}

		return -1;
	}

	/*
	 * breadth-first search for 1. last assignment of the base class 2. nearest
	 * findViewById
	 */
	/*
	private static Map<Value, Unit> bfsCFG(Unit unit, Local value,
			UnitGraph cfg, int model, Value val) {
		Value foundValue = null;
		Unit foundUnit = null;
		Map<Value, Unit> res = new HashMap<>();

		Queue<Unit> queue = new LinkedList<>();
		Set<Unit> visited = new HashSet<>();
		queue.add(unit);
		while (!queue.isEmpty()) {
			int len = queue.size();
			for (int i = 0; i < len; i++) {
				Unit node = queue.poll();
				if (visited.contains(node)) {
					continue;
				}
				visited.add(node);
				switch (model) {
				// r3 = Button(r2)
				case 0:
					if (node instanceof AssignStmt
							&& ((AssignStmt) node).getLeftOp().equals(value)) {
						AssignStmt assignStmt = (AssignStmt) node;
						for (ValueBox box : assignStmt.getRightOp()
								.getUseBoxes()) {
							foundValue = box.getValue();
							break;
						}
						foundUnit = node;
						res.put(foundValue, foundUnit);
						System.out.println("0: " + foundUnit);
						return res;
					}
					break;
				// r2 = findViewById(21...)
				case 1:
					if (node instanceof AssignStmt
							&& (node.toString().contains("findViewById") || node
									.toString().contains("findItem"))
							&& ((AssignStmt) node).getLeftOp().equals(val)) {
						foundValue = val;
						foundUnit = node;
						res.put(foundValue, foundUnit);
						System.out.println("1: " + foundUnit);
						return res;
					}
					break;
				// r3 = getActionView(r7)
				case 2:
					if (node instanceof AssignStmt
							&& (node.toString().contains("getActionView"))) {
						AssignStmt stmt = (AssignStmt) node;
						if (stmt.getLeftOp().equals(val)) {
							InvokeExpr invStmt = ((Stmt) node).getInvokeExpr();
							for (Value arg : invStmt.getArgs()) {
								foundValue = arg;
								foundUnit = node;
								res.put(foundValue, foundUnit);
								System.out.println("2: " + foundUnit);
								return res;
							}
						}
					}
					break;
				}
				for (Unit prev : cfg.getPredsOf(node)) {
					queue.add(prev);
				}
			}
		}

		return res;
	}*/

	public static void permissionAnalysis(String apkDir, String platformDir,
			String extraJar) {
		MySetupApplication app = new MySetupApplication(platformDir, apkDir,
				extraJar);
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

		Scene.v().loadNecessaryClasses();

		// 创建dummy main并作为app的main函数(分析入口)
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

		Transform CGtransform = new Transform("wjtp.checkCG",
				new SceneTransformer() {
					@Override
					protected void internalTransform(String phaseName,
							Map<String, String> options) {
						cg = Scene.v().getCallGraph();
						// icfg = new JimpleBasedInterproceduralCFG();
						getEntries();
						getWidgets();
					}
				});

		PackManager.v().getPack("wjtp").add(CGtransform);
		try {
			System.out
					.println("RUN PACKSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS!");
			PackManager.v().runPacks();
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
