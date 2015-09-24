package FlowDroidPlay;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.xmlpull.v1.XmlPullParserException;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;
import soot.util.dot.DotGraph;

public class CallFlowGraphDot {
	private static DotGraph dot = new DotGraph("callgraph");
	private static HashMap<String, Boolean> visited = new HashMap<String, Boolean>();

	public CallFlowGraphDot() {

	}

	public static void main(String[] args) {

		//File dir = new File("F:/PATDroid/PATDroidTest/app/");
		//File[] files = dir.listFiles();

		//for (int i = 0; i < files.length; i++) {
			//File f = files[i];
			//File f = new File("F:/PATDroid/PATDroidTest/app/app-debug.apk");
		File f = new File("/media/hao/Hitachi/PATDroid/PATDroidTest/app/app-debug.apk");
		String source_apk = f.getAbsolutePath();
		soot.G.reset();

		SetupApplication app = new SetupApplication(
				//"C:/Users/hao/Downloads/android-sdk-windows/platforms",
				"/home/hao/Android/Sdk",
				source_apk);

		try {
			app.calculateSourcesSinksEntrypoints("./SourcesAndSinks.txt");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		Options.v().set_src_prec(Options.src_prec_apk);

		Options.v().set_process_dir(Collections.singletonList(source_apk));
		Options.v().set_force_android_jar(
				//"C:/Users/hao/Downloads/android-sdk-windows/platforms");
				"/home/hao/Android/Sdk/platforms");

		Options.v().set_whole_program(true);

		Options.v().set_allow_phantom_refs(true);

		Options.v().set_output_format(Options.output_for/platformsmat_none);

		Options.v().setPhaseOption("cg.spark verbose:true", "on");

		Scene.v().loadNecessaryClasses();

		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();

		Options.v().set_main_class(entryPoint.getSignature());

		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

		System.out.println("............" + entryPoint.getActiveBody());

		PackManager.v().runPacks();

		System.out.println(Scene.v().getCallGraph().size());
		CallGraph cg = Scene.v().getCallGraph();
		// System.out.println("+++++++++++++++++" + cg);
		// System.out.println(".................."+entryPoint);
		// String label = Scene.v().getCallGraph().listener().toString();
		// dot.createSubGraph(label);
		visit(cg, entryPoint);
		String dest = f.getName();
		String fileNameWithOutExt = FilenameUtils.removeExtension(dest);
		String destination = "./sootOutput/" + fileNameWithOutExt;
		dot.plot(destination + dot.DOT_EXTENSION);
			// soot.PhaseOptions.getBoolean(Scene.v().getCallGraph().listener(),"dump_cg");
			// System.out.println(Scene.v().getCallGraph());

		//}
	}

	private static void visit(CallGraph cg, SootMethod k) {
		String identifier = k.getName();

		visited.put(k.getSignature(), true);

		// System.out.println(dot.drawNode(identifier));
		dot.drawNode(identifier);

		// iterate over unvisited parents
		Iterator<MethodOrMethodContext> ptargets = new Targets(cg.edgesInto(k));

		if (ptargets != null) {
			while (ptargets.hasNext()) {
				SootMethod p = (SootMethod) ptargets.next();

				if (p == null)
					System.out.println("p is null");

				if (!visited.containsKey(p.getSignature()))
					visit(cg, p);
			}
		}

		// iterate over unvisited children
		Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(k));

		if (ctargets != null) {
			while (ctargets.hasNext()) {
				SootMethod c = (SootMethod) ctargets.next();
				if (c == null)
					System.out.println("c is null");
				dot.drawEdge(identifier, c.getName());

				if (!visited.containsKey(c.getSignature()))
					visit(cg, c);
			}
		}
	}
}
