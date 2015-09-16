package FlowDroidPlay;
import java.io.IOException;

import java.util.Collections;

import org.xmlpull.v1.XmlPullParserException;

import soot.PackManager;

import soot.Scene;

import soot.SootMethod;

import soot.jimple.infoflow.android.SetupApplication;

import soot.options.Options;

public class CallFlowGraphTest {

	public CallFlowGraphTest() {

		// TODO Auto-generated constructor stub

	}

	public static void main(String[] args) {

		// TODO Auto-generated method stub

		SetupApplication app = new SetupApplication(
				"D:/AndroidADT/adt-bundle-windows-x86_64-20131030/sdk/platforms",
				"D:/APKs/location.apk");

		try {

			app.calculateSourcesSinksEntrypoints("D:/FlowDroid/SourcesAndSinks.txt");

		} catch (IOException e) {

			// TODO Auto-generated catch block

			e.printStackTrace();

		} catch (XmlPullParserException e) {

			// TODO Auto-generated catch block

			e.printStackTrace();

		}

		soot.G.reset();

		Options.v().set_src_prec(Options.src_prec_apk);

		Options.v().set_process_dir(
				Collections.singletonList("D:/APKs/location.apk"));

		Options.v()
				.set_android_jars(
						"D:/AndroidADT/adt-bundle-windows-x86_64-20131030/sdk/platforms");

		Options.v().set_whole_program(true);

		Options.v().set_allow_phantom_refs(true);

		Options.v().set_output_format(Options.output_format_none);

		Options.v().setPhaseOption("cg.spark", "on");

		Scene.v().loadNecessaryClasses();

		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();

		Options.v().set_main_class(entryPoint.getSignature());

		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

		System.out.println(entryPoint.getActiveBody());

		PackManager.v().runPacks();

		System.out.println(Scene.v().getCallGraph().size());

	}

}