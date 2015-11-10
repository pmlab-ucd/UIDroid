package uiDroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import playSoot.TaintForwardAnalysis;
import soot.Body;
import soot.G;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.NormalUnitPrinter;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.UnitPrinter;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.queue.QueueReader;
import app.MySetupApplication;
import app.MyTest;

public class UiDroidTest extends MyTest {
	private static CallGraph cg;
	private static JimpleBasedInterproceduralCFG icfg;

	public static void main(String[] args) {
		File file = new File(
				"/home/hao/workspace/AppContext/Instrument/InstrumentedApp/ApkSamples/app-debug.apk");
		String apkPath = file.getAbsolutePath();
		String platformPath = "/home/hao/Android/Sdk/platforms";
		String extraJar = "/home/hao/workspace/AppContext/libs";

		permissionAnalysis(apkPath, platformPath, extraJar);
	}

	/*
	 * travel over Call Graph by visit edges one by one
	 */
	public static void analyzeCG() {
		QueueReader<Edge> edges = cg.listener();
		Set<String> visited = new HashSet<>();
		Set<String> activities = new HashSet<>();

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
			MethodOrMethodContext src = edge.getSrc();
			String srcMethod = src.toString();
			String tgtMethod = target.toString();
			if (!visited.contains(srcMethod)) {
				visited.add(srcMethod);
			}
			if (!visited.contains(tgtMethod)) {
				visited.add(tgtMethod);
			}
			
			out.println(src + "  -->   " + target);
			//if (tgtMethod.contains("android.view.View findViewById")) {
			if (tgtMethod.contains("onCreate") && !activities.contains(tgtMethod)) {
				out.print("\n");
				out.println("found an onCreate here!>>>>>>>>>>>>>>>");
				// get callees inside a method body through icfg
				analyzeMethod(target);
				Body body = target.retrieveActiveBody();
				UnitGraph cfg = new ExceptionalUnitGraph(body);
				for (Unit unit : cfg) {
					if (unit instanceof Stmt) {
						Stmt stmt = (Stmt) unit;
						out.println(stmt);
					}
				}
				
				activities.add(tgtMethod);
								
				out.println("end an onCreate >>>>>>>>>>>>>>>");		
				out.print("\n");
			}
		}

		out.println("CG ends==================");
		out.close();
		System.out.println(cg.size());
	}
	
	public static void analyzeMethod(SootMethod method) {
		String sep = File.separator;
		Body body = method.retrieveActiveBody();
		// 生成函数的control flow graph
		UnitGraph cfg = new ExceptionalUnitGraph(body);
		// 执行我们的分析
		UiForwardAnalysis.UiForwardVarAnalysis ta = new UiForwardAnalysis.UiForwardVarAnalysis(cfg);
		// iterate over the results
		for (Unit unit : cfg) {
			//System.out.println(unit);
			List<Local> before = ta.getLiveLocalsBefore(unit);
			List<Local> after = ta.getLiveLocalsAfter(unit);
			UnitPrinter up = new NormalUnitPrinter(body);
			up.setIndent("");
			
			System.out.println("---------------------------------------");		
			unit.toString(up);			
			System.out.println(up.output());
			if (!before.isEmpty()) {
				if (unit.toString().contains("sink")) {
					System.out.println("found a sink!");
				}
			}
			System.out.print("Ui event handlers in: {");
			sep = "";
			for (Local l : before) {
				System.out.print(sep);
				System.out.print(l.getName() + ": " + l.getType());
				sep = ", ";
			}
			System.out.println("}");
			System.out.print("Ui event handlers out: {");
			sep = "";
			for (Local l : after) {
				System.out.print(sep);
				System.out.print(l.getName() + ": " + l.getType());
				sep = ", ";
			}			
			System.out.println("}");			
			System.out.println("---------------------------------------");
		}
	}

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
						icfg = new JimpleBasedInterproceduralCFG();
						analyzeCG();
					}
				});

		PackManager.v().getPack("wjtp").add(CGtransform);
		PackManager.v().runPacks();
	}
}
