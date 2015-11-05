package playAppContext;

import java.io.BufferedReader;
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
import java.util.Set;

import org.apache.commons.io.FileUtils;
//import org.jboss.util.collection.ConcurrentReferenceHashMap.Option;
import org.xmlpull.v1.XmlPullParserException;

import app.Context;
import app.DFSPathQueue;
import app.PermissionInvocation;
import au.com.bytecode.opencsv.CSVWriter;
import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.InfoflowResults.SinkInfo;
import soot.jimple.infoflow.android.TestApps.Test;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
//import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.util.queue.QueueReader;
import soot.jimple.infoflow.InfoflowResults.SourceInfo;

public class MyTest extends Test {
	// the list of sensitive methods: got from Pscout
	private static List<String> PscoutMethod;
	static String appName;
	private static String csvName;
	private static CallGraph cg;
	private static JimpleBasedInterproceduralCFG icfg;
	private static List<PermissionInvocation> perInvocs = new ArrayList();
	// control flow graph
	private static IInfoflowCFG flowcfg;
	// data flow analysis
	private static soot.jimple.infoflow.InfoflowResults flowResults;

	public static void main(String[] args) throws IOException,
			InterruptedException {
		myTestMain(args);
	}

	private static final class ConditionalResultsAvailableHandler implements
			ResultsAvailableHandler {
		IInfoflowCFG cfg;

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg,
				soot.jimple.infoflow.InfoflowResults results) {
			this.cfg = (IInfoflowCFG) cfg;
			MyTest.flowcfg = (IInfoflowCFG) cfg;
			System.out.println("end flow analysis: " + new Date());

			if (results == null) {
				System.out.println("No result found.");
			} else {
				MyTest.flowResults = results;
			}

		}

		public boolean isSameStmt(Stmt conStmt, Stmt sink) {
			if (conStmt.getInvokeExpr().getMethod().toString()
					.equals(sink.getInvokeExpr().getMethod().toString())) {
				String conCaller = MyTest.icfg.getMethodOf(conStmt).toString();

				String sinkCaller = ((SootMethod) cfg.getMethodOf(sink))
						.toString();

				if (conCaller.equals(sinkCaller))
					return true;
			}
			return false;
		}
	}

	/*
	 * 参考自Test.java中的main()
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
		// 额外的lib
		String extraJar = args[3];

		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()) {
			boolean success = true;
			File[] arrayOfFile;
			// 获得文件夹下文件数目
			int j = (arrayOfFile = outputDir.listFiles()).length;
			// 遍历文件
			for (int i = 0; i < j; i++) {
				File f = arrayOfFile[i];
				// 用于删除目录下所有文件
				success = (success) && (f.delete());
				if (!success) {
					System.err.println("Cleanup of output dir " + outputDir
							+ " failed@");
				}
				outputDir.delete();
			}
		}

		// 读入额外的参数
		if (!parseAdditionalOptions(args)) {
			return;
		}
		if (!validateAdditionalOptions()) {
			return;
		}
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
			for (String s : dirFiles)
				apkFiles.add(s);
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

		for (final String fileName : apkFiles) {
			final String fullFilePath;
			System.gc();

			// Directory handling
			if (apkFiles.size() > 1) {
				if (apkFile.isDirectory())
					fullFilePath = args[0] + File.separator + fileName;
				else
					fullFilePath = fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
				File flagFile = new File("_Run_" + new File(fileName).getName());
				if (flagFile.exists())
					continue;
				flagFile.createNewFile();
			} else
				fullFilePath = fileName;

			// Run the taint analysis
			System.gc();
			// will assign results to flowResults
			if (timeout > 0) {
				runAnalysisTimeout(fullFilePath, args[1]);
			} else if (sysTimeout > 0) {
				runAnalysisSysTimeout(fullFilePath, args[1]);
			} else {
				// 这里多了个extraJar参数
				runAnalysis(fullFilePath, args[1], extraJar);
			}
			// AppContext独有的
			permissionAnalysis(fullFilePath, args[1], extraJar);

			System.gc();
		}
	}

	private static void permissionAnalysis(String apkDir, String platformDir,
			String extraJar) {
		MySetupApplication app = new MySetupApplication(platformDir, apkDir,
				extraJar);
		try {
			app.calculateSourcesSinksEntrypoints("./SourceAndSinks.txt");
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
		SootClass c = Scene.v().forceResolve(
				"org.bouncycastle.asn1.DERObjectIdentifier", 3);
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
				System.out.println(target.toString());
				PermissionInvocation perInvoc = new PermissionInvocation(
						(SootMethod) src, target);
				if (!perInvocs.contains(perInvoc)) {
					System.out.println("begin per analysis: " + new Date());
					// 获得敏感函数所对应的权限
					perInvoc.setPermission(getPermissionForInvoc(
							target.toString(), PscoutMethod));
					printCFGpath((SootMethod) src, target, icfg, cg, perInvoc);
					System.out.println("end per analysis: " + new Date());
					perInvocs.add(perInvoc);
					analyzeFlowResult(perInvoc);
				}
			}
		}
		
		System.out.println("size: " + perInvocs.size() + "cg.size: " + cg.size());
		out.println("CG ends==================");
		out.close();
	}

	private static void printCFGpath(SootMethod src, SootMethod tgt,
			BiDiInterproceduralCFG<Unit, SootMethod> icfg, CallGraph cg,
			PermissionInvocation perInvoc) {
		// iterate over the statements where source methods locate
		Set<Unit> callers = icfg.getCallsFromWithin(src);
		Iterator<Unit> unitItr = callers.iterator();
		Stmt tgtStmt = null;
		while (unitItr.hasNext()) {
			Unit u = (Unit) unitItr.next();
			if ((u instanceof Stmt)) {
				Stmt stmt = (Stmt) u;
				if ((stmt.containsInvokeExpr())
						&& stmt.getInvokeExpr().getMethod().equals(tgt)) {
					tgtStmt = stmt;
				}
			}
			System.out.println("====CFG====");
			printCFGpath(src, tgtStmt, icfg, cg, perInvoc);
		}

	}

	private static void printCFGpath(SootMethod src, Unit u,
			BiDiInterproceduralCFG<Unit, SootMethod> icfg, CallGraph cg,
			PermissionInvocation permInvoc) {
		Set<Unit> callers = new HashSet<>();
		Unit last = null;
		// store visited by DFS
		DFSPathQueue<Unit> unitStack = new DFSPathQueue<Unit>();
		DFSPathQueue<SootMethod> callerStack = new DFSPathQueue<SootMethod>();
		Map<Unit, SootMethod> contexts = new HashMap<Unit, SootMethod>();
		ArrayList<SootMethod> path = new ArrayList<SootMethod>();
		path.add(src);
		ArrayList<Set<SootMethod>> methodByEntries = new ArrayList<Set<SootMethod>>();
		ArrayList<SootMethod> entries = new ArrayList<SootMethod>();
		unitStack.push(u);
		callerStack.push(src);
		int signal = 0;
		Set<SootMethod> s;
		while (!unitStack.isEmpty()) {
			boolean isStartpoint = true;
			try {
				isStartpoint = icfg.isStartPoint(u);
			} catch (NullPointerException e) {
				System.err.println("DirectedGraph cannot be constructed: " + u);
			}
		}

	}

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

	private static void analyzeFlowResult(PermissionInvocation perInvoc) {
		System.out.println("tgt: " + perInvoc.getTgt());
		System.out.println("src: " + perInvoc.getSrc());
		System.out.println("per: " + perInvoc.getPermission());

		System.out.println("=======CFG======");
		/*
		 * Iterator localIterator2, localIterator4; SootMethod m; // iterate
		 * over flowResults for (Iterator localIterator1 =
		 * flowResults.getResults().keySet().iterator();
		 * localIterator1.hasNext(); localIterator2.hasNext()) {
		 * soot.jimple.infoflow.InfoflowResults.SourceInfo sink =
		 * (soot.jimple.infoflow
		 * .InfoflowResults.SourceInfo)localIterator1.next(); //
		 * 此处的context是指sink所在的语句 Stmt context = sink.getContext();
		 * System.out.println("Found a flow to sink" + sink + " Context: " +
		 * context); localIterator2 = perInvoc.getContexts().iterator();
		 * //continue; Context ctx = (Context)localIterator2.next();
		 * System.out.println("Entry: " + ctx); }
		 */
		Iterator<Context> localIterator2;
		Iterator<Set> localIterator4;
		SootMethod m;
		for (Iterator<SinkInfo> localIterator1 = flowResults.getResults().keySet()
				.iterator(); localIterator1.hasNext(); localIterator2.hasNext()) {
			InfoflowResults.SinkInfo sink = (InfoflowResults.SinkInfo) localIterator1
					.next();

			Stmt context = sink.getContext();
			System.out.println("Found a flow to sink " + sink + "  Context: "
					+ context);
			localIterator2 = perInvoc.getContexts().iterator();
			//continue;
			Context ctx = (Context) localIterator2.next();
			System.out.println("Entry: " + ctx.getEntrypoint());
			for (Stmt conStmt : ctx.getConditionalStmt()) {
				if (isSameStmt(conStmt, context)) {
					localIterator4 = ((Set) flowResults.getResults().get(sink)).iterator();
					while (localIterator4.hasNext()) {
						InfoflowResults.SourceInfo source = (InfoflowResults.SourceInfo) localIterator4
								.next();

						Value factorValue = source.getSource();
						if ((factorValue instanceof InvokeExpr)) {
							InvokeExpr factorExpr = (InvokeExpr) factorValue;
							m = factorExpr.getMethod();
							if (!ctx.hasFactorMethod(m))
								ctx.addFactorMethod(m);
							System.out.println("factor method: "
									+ factorExpr.getMethod());
						} else if ((factorValue instanceof Ref)) {
							Ref factorRef = (Ref) factorValue;
							if (!ctx.hasFactorRef(factorRef))
								ctx.addFactorRef(factorRef);
							System.out.println("Ref factor: "
									+ source.getSource());
						} else {
							System.out.println("Other factor: "
									+ source.getSource().getType());
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
				String[] resultArray = (String[]) result
						.toArray(new String[result.size()]);
				data.add(resultArray);
			}

			writer.writeAll(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
