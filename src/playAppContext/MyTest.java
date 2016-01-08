package playAppContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import com.opencsv.CSVWriter;

import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.results.xml.InfoflowResultsSerializer;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.util.queue.QueueReader;

/**
 * @ClassName: MyTest
 * @Description: The helper class of Main to run analysis.
 * @author: Hao Fu
 * @date: Dec 28, 2015 8:22:49 PM
 */
public class MyTest extends playFlowDroid.Test {
	/**
	 * @Fields PscoutMethod : The list of sensitive methods: got from Pscout
	 */
	private static List<String> PscoutMethod;
	static String appName;
	private static String csvName;
	private static CallGraph cg;
	private static JimpleBasedInterproceduralCFG icfg;
	private static List<PermissionInvocation> perInvocs = new ArrayList<>();
	// control flow graph
	private static IInfoflowCFG flowcfg;
	// data flow analysis
	private static InfoflowResults flowResults;

	protected final static BufferedWriter wr = null;

	public static void main(String[] args) throws IOException,
			InterruptedException {
		myTestMain(args);
	}

	/**
	 * @ClassName: ConditionalResultsAvailableHandler
	 * @Description: Context factor collector
	 * @author: Hao Fu
	 * @date: Dec 28, 2015 8:27:31 PM
	 */
	private static final class ConditionalResultsAvailableHandler implements
			ResultsAvailableHandler {
		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			MyTest.flowcfg = cfg;
			print("end flow analysis: " + new Date());

			if (results == null) {
				print("No result found.");
			} else {
				MyTest.flowResults = results;
			}
		}
	}

	/**
	 * @Title: myTestMain
	 * @Description: 参考自Test.java中的main()
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 * @return: void
	 */
	public static void myTestMain(String[] args) throws IOException,
			InterruptedException {
		// 读入Pscout
		PscoutMethod = FileUtils.readLines(new File(
				"./jellybean_publishedapimapping_parsed.txt"));
		// 如参数过少， 打印帮助
		if (args.length < 2) {
			printUsage();
			return;
		}
		// 输出成csv格式
		csvName = args[2];

		// start with cleanup:
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()) {
			boolean success = true;
			for (File f : outputDir.listFiles()) {
				success = success && f.delete();
			}
			if (!success) {
				System.err.println("Cleanup of output directory " + outputDir
						+ " failed!");
			}
			outputDir.delete();
		}

		// Parse additional command-line arguments
		if (!parseAdditionalOptions(args))
			return;
		if (!validateAdditionalOptions())
			return;

		List<String> apkFiles = new ArrayList<String>();
		File apkFile = new File(args[0]);
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}

			});
			for (String s : dirFiles)
				apkFiles.add(s);
		} else {
			// apk is a file so grab the extension
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

		for (final String fileName : apkFiles) {
			String fullFilePath = fileName;

			// Directory handling
			if (apkFiles.size() >= 1) {
				if (apkFile.isDirectory())
					fullFilePath = args[0] + File.separator + fileName;
				else
					fullFilePath = fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
			} else
				fullFilePath = fileName;

			appName = fullFilePath;
			// Run the taint analysis
			System.gc();
			// will assign results to flowResults
			String path = null;
			if (timeout > 0) {
				runAnalysisTimeout(fullFilePath, args[1]);
			} else if (sysTimeout > 0) {
				runAnalysisSysTimeout(fullFilePath, args[1]);
			} else {
				// AppContext's
				//path = System.getProperty ("java.home" ) + File.separator + "lib" + File.separator
                       // + "rt.jar" + File. pathSeparator + "." + File.separator + "libs" ;
				path = "." + File.separator + "libs";
				runAnalysis(fullFilePath, args[1], path,
						new ConditionalResultsAvailableHandler());
			}
			// AppContext独有的, leverage results of taint analysis to identify
			// context factors inside conditional stmts
			permissionAnalysis(fullFilePath, args[1]);
			print(path);
			System.gc();
		}
	}

	public static void print(String string) {
		try {
			System.out.println("[AppContext] " + string);
			if (wr != null) {
				wr.write(string + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @Title: runAnalysis
	 * @Description: Taint Analysis module
	 * @param fileName
	 * @param androidJar
	 * @param extraJar
	 * @return
	 * @return: InfoflowResults
	 */
	public static InfoflowResults runAnalysis(String fileName,
			String androidJar, String extraJar,
			ResultsAvailableHandler resHandler) {
		try {
			print("RUN ANALYSIS");
			final long beforeRun = System.nanoTime();

			final SetupApplication app;
			app = new SetupApplication(androidJar, fileName, extraJar,
					ipcManager);

			// Set configuration object
			// Support layout mode. --Hao
			config.setLayoutMatchingMode(LayoutMatchingMode.MatchAll);
			// ctx sensitive
			config.setPathBuilder(PathBuilder.ContextSensitive);
			// --safemode
			InfoflowAndroidConfiguration.setUseThisChainReduction(false);
			app.setConfig(config);

			final ITaintPropagationWrapper taintWrapper;
			if (noTaintWrapper)
				taintWrapper = null;
			else if (summaryPath != null && !summaryPath.isEmpty()) {
				System.out.println("Using the StubDroid taint wrapper");
				taintWrapper = createLibrarySummaryTW();
				if (taintWrapper == null) {
					System.err.println("Could not initialize StubDroid");
					return null;
				}
			} else {
				final EasyTaintWrapper easyTaintWrapper;
				if (new File("../soot-infoflow/EasyTaintWrapperSource.txt")
						.exists())
					easyTaintWrapper = new EasyTaintWrapper(
							"../soot-infoflow/EasyTaintWrapperSource.txt");
				else
					easyTaintWrapper = new EasyTaintWrapper(
							"EasyTaintWrapperSource.txt");
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper = easyTaintWrapper;
			}
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");

			if (DEBUG) {
				app.printEntrypoints();
				app.printSinks();
				app.printSources();
			}

			System.out.println("Running data flow analysis...");
			final InfoflowResults res = app.runInfoflow(resHandler);
			System.out.println("Analysis has run for "
					+ (System.nanoTime() - beforeRun) / 1E9 + " seconds");

			// Write the results into a file if requested
			if (resultFilePath != null && !resultFilePath.isEmpty()) {
				InfoflowResultsSerializer serializer = new InfoflowResultsSerializer();
				serializer.serialize(res, resultFilePath);
			}

			return res;
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: "
					+ ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XMLStreamException ex) {
			System.err.println("Could not write data flow results to file: "
					+ ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	/**
	 * @Title: permissionAnalysis
	 * @Description: The wrapper of analyzeCG
	 * @param apkDir
	 * @param platformDir
	 * @param extraJar
	 * @return: void
	 */
	private static void permissionAnalysis(String apkDir, String platformDir) {
		// cg will not be complete if directly use app from runAnalysis().
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
		Options.v().set_src_prec(5);
		Options.v().set_process_dir(Collections.singletonList(apkDir));
		Options.v().set_android_jars(platformDir);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(13);

		Scene.v().loadNecessaryClasses();
		// 干嘛用的？
		// SootClass c = Scene.v().forceResolve(
		// "org.bouncycastle.asn1.DERObjectIdentifier", 3);
		// 创建dummy main并作为app的main函数(分析入口)
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
		Options.v().set_main_class(entryPoint.getSignature());
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

		Transform CGtransform = new Transform("wjtp.checkCG",
				new SceneTransformer() {
					@Override
					protected void internalTransform(String phaseName,
							Map<String, String> options) {
						MyTest.cg = Scene.v().getCallGraph();
						MyTest.icfg = new JimpleBasedInterproceduralCFG();
						MyTest.analyzeCG();
					}
				});

		PackManager.v().getPack("wjtp").add(CGtransform);
		PackManager.v().runPacks();
	}

	/**
	 * @Title: analyzeCG
	 * @Description: Traverse the call graph and identify sensitive api calls
	 * @return: void
	 */
	private static void analyzeCG() {
		QueueReader<Edge> edges = cg.listener();

		File resultFile = new File("CG2.log");
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
			out.println(src + "  -->   " + target);
			// 如果是敏感函数
			if (PscoutMethod.contains(target.toString())) {
				print(target.toString());
				PermissionInvocation perInvoc = new PermissionInvocation(
						(SootMethod) src, target);
				if (!perInvocs.contains(perInvoc)) {
					print("=======================");
					print("begin permission analysis: " + new Date());
					// 获得敏感函数所对应的权限
					perInvoc.setPermission(getPermissionForInvoc(
							target.toString(), PscoutMethod));
					// print the cf path towards to the dummyMain
					// of the sensitive method in icfg
					printCFGpath((SootMethod) src, target, icfg, cg, perInvoc);
					System.out
							.println("end permission analysis: " + new Date());
					print("=======================");
					perInvocs.add(perInvoc);
					analyzeFlowResult(perInvoc);
				}
			}
		}

		print("/**********END OF THE APP***********/");
		print("perInvocs.size: " + perInvocs.size() + "; cg.size: " + cg.size());
		out.println("CG ends==================");
		out.close();
	}

	/**
	 * @Title: printCFGpath
	 * @Descrption: Extract the control flow and cg path of the tgt methods
	 *              towards to the dummyMain in icfg/cg; Get relevant contexts:
	 *              entry points and conditional stmts along the path
	 * @param src
	 *            : caller of the sensitive API
	 * @param tgt
	 *            : the sensitive API call
	 * @param icfg
	 *            : interprocedure cfg
	 * @param cg
	 *            : call graph
	 * @param perInvoc
	 *            : permission invocation
	 * @return: void
	 * @throws
	 */
	private static void printCFGpath(SootMethod src, SootMethod tgt,
			BiDiInterproceduralCFG<Unit, SootMethod> icfg, CallGraph cg,
			PermissionInvocation perInvoc) {
		// extract all call sites inside the src method
		Set<Unit> callers = icfg.getCallsFromWithin(src);
		Iterator<Unit> unitItr = callers.iterator();
		Stmt tgtStmt = null;
		// iterate over the call sites and print the cfg when it is tgt
		while (unitItr.hasNext()) {
			Unit u = (Unit) unitItr.next();
			if ((u instanceof Stmt)) {
				Stmt stmt = (Stmt) u;
				if ((stmt.containsInvokeExpr())
						&& stmt.getInvokeExpr().getMethod().equals(tgt)) {
					tgtStmt = stmt;
				}
			}
		}

		print("=========CFG=======");
		printCFGpath(src, tgtStmt, icfg, cg, perInvoc);
	}

	/**
	 * @Title: cutPath
	 * @Description: Cut path by rm nodes not equal to "node".
	 * @param path
	 * @param node
	 * @return: ArrayList<SootMethod>
	 */
	public static ArrayList<SootMethod> cutPath(ArrayList<SootMethod> path,
			SootMethod node) {
		int length = path.size() - 1;
		for (int i = length; i >= 0; i--) {
			if (!((SootMethod) path.get(i)).equals(node)) {
				path.remove(i);
			}
		}
		return path;
	}

	/**
	 * @Title: printCFGpath
	 * @Description: Extract the control flow/cg path to dummyMain of the tgt
	 *               method, which is stored in u, in icfg. Store entry point
	 *               and conditional stmts along the path.
	 * @param src
	 *            : caller
	 * @param u
	 *            : the stmt where tgt method (callee/sens) sits
	 * @param icfg
	 * @param cg
	 * @param permInvoc
	 * @return: void
	 * @throws
	 */
	private static void printCFGpath(SootMethod src, Unit u,
			BiDiInterproceduralCFG<Unit, SootMethod> icfg, CallGraph cg,
			PermissionInvocation permInvoc) {
		Set<Unit> callers = new HashSet<>();
		// Unit last = null;
		// store visited nodes in a queue, dfs the path to src.
		// each unit in unit stack represents a method along the path from
		// dummyMain to tgt
		DFSPathQueue<Unit> unitStack = new DFSPathQueue<>();
		DFSPathQueue<SootMethod> callerStack = new DFSPathQueue<>();
		// context:: condition unit : method
		Map<Unit, SootMethod> contexts = new HashMap<>();
		// cg path from src to entry?
		ArrayList<SootMethod> path = new ArrayList<>();
		path.add(src);
		ArrayList<Set<SootMethod>> methodByEntries = new ArrayList<>();
		ArrayList<SootMethod> entries = new ArrayList<>();
		unitStack.push(u);
		callerStack.push(src);
		// to count the pred of
		int signal = 0;
		Set<SootMethod> s;
		// int tmp = 1;
		// step forward from tgt to dummyMain
		while (!unitStack.isEmpty()) {
			// print(u + " " + tmp++);
			boolean isStartpoint = true;
			try {
				// Returns true is this is a method's start statement.
				isStartpoint = icfg.isStartPoint(u);
			} catch (NullPointerException e) {
				System.err.println("DirectedGraph cannot be constructed: " + u);
				try {
					u = (Unit) icfg.getPredsOf(u).iterator().next();
				} catch (NullPointerException e1) {
					isStartpoint = true;
				}
				// last = u;
			}
			if (!isStartpoint) {
				// if is a condition stmt
				if (((u instanceof IfStmt)) || ((u instanceof TableSwitchStmt))
						|| ((u instanceof LookupSwitchStmt))) {
					// print("CONDITIONAL STMT: " + u);
					// check whether the pred node is a conditional stmt
					// and this node is conditional dep on pred node
					if (signal <= 0) {
						// print("Signal <= 0");
						Unit predUnit = u;
						while (u.equals(predUnit)) {
							// THE single pred node
							predUnit = (Unit) icfg.getPredsOf(u).iterator()
									.next();
							// print("PRED STMT: " + predUnit);
							// make sure not recursion?
							if ((icfg.getPredsOf(u).size() == 1)
									&& ((predUnit instanceof InvokeStmt))) {
								InvokeStmt condStmt = (InvokeStmt) predUnit;
								if (condStmt.getInvokeExpr().getMethod()
										.getName().contains("invokeIfStmt")) {
									u = predUnit;
									contexts.put(condStmt, src);
									// print("context: " + condStmt + " of " +
									// src);
								}
							}
						}
					} else {
						signal--;
					}
				}
				icfg.getPredsOf(u).size();

				// avoid stuck at loop or recursion
				if (icfg.getPredsOf(u).size() > 1) {
					signal++;
					// print("Signal++:" + u);
					// for (Unit unit : icfg.getPredsOf(u)) {
					// print("Signal: " + unit);
					// }
				}

				// last = u;
				// iterate towards the first stmt
				u = (Unit) icfg.getPredsOf(u).iterator().next();
			} else {
				// if is the first stmt of a method
				Iterator<Edge> iter = cg.edgesInto(src);
				while (iter.hasNext()) {
					Edge edge = (Edge) iter.next();
					SootMethod srcCallerMethod = edge.src();
					if (srcCallerMethod.toString().contains(
					// dummyMain is the ultimate main in cg gen by FlowDr
							"dummyMainClass: void dummyMainMethod")) {
						if (!entries.contains(src)) {
							entries.add(src);
							methodByEntries.add(new HashSet<>(path));
						} else {
							int i = entries.indexOf(src);
							Set<SootMethod> set = (Set<SootMethod>) methodByEntries
									.get(i);
							set.addAll(new HashSet<>(path));
							methodByEntries.set(i, set);
						}

						if (callerStack.lastRemoved() == null)
							break;
						path = cutPath(path,
								(SootMethod) callerStack.lastRemoved());

						break;
					}
					Unit caller = edge.srcUnit();
					if (caller != null) {
						if (!callers.contains(caller)) {
							callers.add(caller);
							if (!caller.toString().contains(
									"dummyMainClass: void dummyMainMethod()")) {
								// add caller (pred nodes) to the queue
								unitStack.push(caller);
								callerStack.push(srcCallerMethod);
							}
						} else {
							for (int i = 0; i < methodByEntries.size(); i++) {
								s = methodByEntries.get(i);
								if (s.contains(srcCallerMethod)) {
									s.addAll(new HashSet<>(path));
									methodByEntries.set(i, s);
								}
							}
							if (callerStack.lastRemoved() != null)
								path = cutPath(path,
										(SootMethod) callerStack.lastRemoved());
						}
					}
				}
				// done analyze this unit
				u = (Unit) unitStack.pop();
				src = (SootMethod) callerStack.pop();
				path.add(src);
				// print("Path:" + path);
			}
			// print("Path:" + path);
		}

		// Set permission contexts
		// iterate over entry point and set entry point and corresponding pred
		// conditional stmts as perm ctx
		for (int i = 0; i < methodByEntries.size(); i++) {
			Set<SootMethod> set = methodByEntries.get(i);
			SootMethod m = (SootMethod) entries.get(i);
			Context ctx = new Context();
			List<Stmt> conditionalStmt = new ArrayList<>();
			ctx.setEntrypoint(m);

			for (SootMethod method : set) {
				for (Entry<Unit, SootMethod> context : contexts.entrySet()) {
					if (((SootMethod) context.getValue()).equals(method)) {
						conditionalStmt.add((Stmt) context.getKey());
					}
				}
			}

			ctx.setConditionalStmt(conditionalStmt);
			permInvoc.addContext(ctx);
		}
	}

	/**
	 * @Title: getPermissionForInvoc
	 * @Description: API call => Permission
	 * @param signature
	 * @param file
	 * @return: String
	 */
	private static String getPermissionForInvoc(String signature,
			List<String> file) {
		String permission = "";

		for (String s : file) {
			if (s.startsWith("Permission:")) {
				permission = s.substring(11, s.length());
			} else if (s.contains(signature)) {
				break;
			}
		}

		return permission;
	}

	public static boolean isSameStmt(Stmt conStmt, Stmt sink) {
		if (conStmt.getInvokeExpr().getMethod().toString()
				.equals(sink.getInvokeExpr().getMethod().toString())) {
			String conCaller = icfg.getMethodOf(conStmt).toString();

			String sinkCaller = ((SootMethod) flowcfg.getMethodOf(sink))
					.toString();

			if (conCaller.equals(sinkCaller))
				return true;
		}
		return false;
	}

	/**
	 * @Title: analyzeFlowResult
	 * @Description: Collecting flow results
	 * @param perInvoc
	 * @return: void
	 */
	private static void analyzeFlowResult(PermissionInvocation perInvoc) {
		print("=======Write Results To CSV======");
		print("tgt: " + perInvoc.getTgt());
		print("src: " + perInvoc.getSrc());
		print("per: " + perInvoc.getPermission());

		SootMethod m;
		for (ResultSinkInfo sink : flowResults.getResults().keySet()) {
			// print("Found a flow to sink: " + sink);
			for (Context ctx : perInvoc.getContexts()) {
				// print("Entry: " + ctx.getEntrypoint());
				// 此处的context是指sink所在的语句
				Stmt sinkStmt = sink.getSink();
				for (Stmt conStmt : ctx.getConditionalStmt()) {
					// print("*********condistmt: " + conStmt.toString());
					// when sink == conStmt, source == natural env vars
					if (isSameStmt(conStmt, sinkStmt)) {
						// print("Context:: Conditional Factors: ");
						for (ResultSourceInfo source : flowResults.getResults()
								.get(sink)) {
							// print("Srcs: " + source);
							Stmt factorValue = source.getSource();
							if ((factorValue instanceof InvokeExpr)) {
								InvokeExpr factorExpr = (InvokeExpr) factorValue;
								m = factorExpr.getMethod();
								if (!ctx.hasFactorMethod(m)) {
									ctx.addFactorMethod(m);
								}
								// print("factor method: "
								// + factorExpr.getMethod());
							} else if ((factorValue instanceof Ref)) {
								Ref factorRef = (Ref) factorValue;
								if (!ctx.hasFactorRef(factorRef)) {
									ctx.addFactorRef(factorRef);
								}
								// print("Ref factor: " + source.getSource());
							} else {
								// print("Other factor: " + source.getSource());
								if (!ctx.hasOtherFactor(factorValue)) {
									ctx.addOtherFactor(factorValue);
								}
							}
						}
					}
				}
			}
		}

		String csv = csvName;
		try {
			File csvFile = new File(csv);
			if (!csvFile.exists()) {
				csvFile.createNewFile();
			}

			CSVWriter writer = new CSVWriter(new FileWriter(csv, true));

			List<String[]> data = new ArrayList<>();

			String permission = perInvoc.getPermission();
			String tgt = perInvoc.getTgt().toString();

			String entrypoint = "";
			for (Context ctx : perInvoc.getContexts()) {
				ArrayList<String> result = new ArrayList<>();
				entrypoint = ctx.getEntrypoint().toString();
				result.add(appName);
				result.add(permission);
				result.add(tgt);
				result.add(entrypoint);
				if (ctx.getFactorMethod() != null) {
					for (SootMethod method : ctx.getFactorMethod()) {
						result.add(method.toString());
					}
				}
				if (ctx.getFactorRef() != null) {
					for (Ref r : ctx.getFactorRef()) {
						result.add(r.toString());
					}
				}
				if (ctx.getOtherFactor() != null) {
					for (Stmt s : ctx.getOtherFactor()) {
						result.add(s.toString());
					}
				}
				String[] resultArray = (String[]) result
						.toArray(new String[result.size()]);
				data.add(resultArray);
				print("RESULT" + result.toString());
			}

			writer.writeAll(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}