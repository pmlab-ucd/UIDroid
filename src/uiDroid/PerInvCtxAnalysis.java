/**
 * Copyright 2016 Hao Fu. All rights Reserved 
 * @Title: 	PerInvCtxAnalysis.java 
 * @Package uiDroid 
 * @Description: 	TODO(用一句话描述该文件做什么) 
 * @author:	Hao Fu 
 * @date:	Jan 2, 2016 9:17:18 PM 
 * @version	V1.0   
 */
package uiDroid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import playAppContext.Context;
import playAppContext.DFSPathQueue;
import playAppContext.PermissionInvocation;
import playGator.Config;
import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
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
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.util.queue.QueueReader;

import com.opencsv.CSVWriter;

/**
 * @ClassName: PerInvCtxAnalysis
 * @Description: Permission Invocation Contexts Analysis
 * @author: Hao Fu
 * @date: Jan 2, 2016 9:17:18 PM
 */
public class PerInvCtxAnalysis {
	// sensitive permission related
	private static List<String> PscoutMethod;
	static String appName;
	private static String csvName = "./sootOutput/output.csv";
	private static CallGraph cg;
	private static JimpleBasedInterproceduralCFG icfg;
	public static List<PermissionInvocation> perInvocs = new ArrayList<>();
	// control flow graph
	private static IInfoflowCFG flowcfg;
	// data flow analysis
	private static InfoflowResults flowResults;
	private static boolean debug = true;
	protected final static BufferedWriter wr = null;
	
	/**
	 * @ClassName: SingletonHolder
	 * @Description: The nested class to implement singleton
	 * @author: Hao Fu
	 * @date: Jan 2, 2016 9:28:25 PM
	 */
	private static class SingletonHolder {
		private static final PerInvCtxAnalysis instance = new PerInvCtxAnalysis();
	}

	/**
	 * @Title: v
	 * @Author: hao
	 * @Description: Get THE instance
	 * @return
	 * @return: PerInvCtxAnalysis
	 */
	public static final PerInvCtxAnalysis v() {
		return SingletonHolder.instance;
	}

	public static void print(String string) {
		if (!debug) {
			return;
		}
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
	 * @ClassName: ConditionalResultsAvailableHandler
	 * @Description: Context factor collector
	 * @author: Hao Fu
	 * @date: Dec 28, 2015 8:27:31 PM
	 */
	private static final class ConditionalResultsAvailableHandler implements
			ResultsAvailableHandler {
		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			flowcfg = cfg;
			print("end flow analysis: " + new Date());

			if (results == null) {
				print("No result found.");
			} else {
				flowResults = results;
			}
		}
	}

	public void run() {
		// Taint Analysis
		System.gc();
		playAppContext.MyTest.runAnalysis(Config.apkPath, Config.platformDir, "./libs/",
				new ConditionalResultsAvailableHandler());
		if (flowcfg == null || flowResults == null) {
			throw new RuntimeException();
		}
		// Permission analysis
		permissionAnalysis();
	}

	/**
	 * @Title: permissionAnalysis
	 * @Description: The wrapper of analyzeCG
	 * @param apkDir
	 * @param platformDir
	 * @param extraJar
	 * @return: void
	 */
	public void permissionAnalysis() {
		// cg will not be complete if directly use app from runAnalysis().
		SetupApplication app = new SetupApplication(Config.platformDir,
				Config.apkPath);
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
		// This statement is necessary to make XMLParser in gator to run
		// correcly
		Scene.v().addBasicClass("android.widget.RelativeLayout",
				SootClass.SIGNATURES);
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
						icfg = new JimpleBasedInterproceduralCFG();
						analyzeCG();
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
		// step forward from tgt to dummyMain
		while (!unitStack.isEmpty()) {
			print(u + ": " + u.getJavaSourceStartLineNumber());
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
					print("CONDITIONAL STMT: " + u);
					// check whether the pred node is a conditional stmt
					// and this node is conditional dep on pred node
					if (signal <= 0) {
						print("Signal <= 0");
						Unit predUnit = u;
						while (u.equals(predUnit)) {
							// THE single pred node
							predUnit = (Unit) icfg.getPredsOf(u).iterator()
									.next();
							print("PRED STMT: " + predUnit);
							// make sure not recursion?
							if ((icfg.getPredsOf(u).size() == 1)
									&& ((predUnit instanceof InvokeStmt))) {
								InvokeStmt condStmt = (InvokeStmt) predUnit;
								if (condStmt.getInvokeExpr().getMethod()
										.getName().contains("invokeIfStmt")) {
									u = predUnit;
									contexts.put(condStmt, src);
									print("context stmt: " + condStmt + " of " + src);
								}
							}
						}
					} else {
						signal--;
					}
				}

				// avoid stuck at loop or recursion
				if (icfg.getPredsOf(u).size() > 1) {
					//signal++;
					//print("Signal++:" + u);
					/*
					int count  = 0;
					for (Unit unit : icfg.getPredsOf(u)) {
						count++;
					 print("Signal: " + unit);
					 Unit tmpp = icfg.getPredsOf(unit).iterator().next();
					 Unit prior = unit;
					 while (tmpp != prior) {
						 print("Sub " + count + ": " + tmpp);
						 prior = tmpp;
						 if (icfg.getPredsOf(tmpp).iterator().hasNext())
						 tmpp = icfg.getPredsOf(tmpp).iterator().next();
					 }
					}*/
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
									"dummyMainClass: void dummyMainMethod")) {
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
		for (Context ctx : perInvoc.getContexts()) {
			// print("Entry: " + ctx.getEntrypoint());
			// 此处的context是指sink所在的语句
			for (Stmt conStmt : ctx.getConditionalStmt()) {
				print("*********condistmt: " + conStmt.toString());
				for (ResultSinkInfo sink : flowResults.getResults().keySet()) {
					//print("Found a flow to sink: " + sink);		
					// when sink == conStmt, source == natural env vars
					Stmt sinkStmt = sink.getSink();
					if (isSameStmt(conStmt, sinkStmt)) {
						print("Found a flow to sink: " + sink);	
						print("Context:: Conditional Factors: ");
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
								print("factor method: "
										+ factorExpr.getMethod());
							} else if ((factorValue instanceof Ref)) {
								Ref factorRef = (Ref) factorValue;
								if (!ctx.hasFactorRef(factorRef)) {
									ctx.addFactorRef(factorRef);
								}
								print("Ref factor: " + source.getSource());
							} else {
								print("Other factor: " + source.getSource());
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