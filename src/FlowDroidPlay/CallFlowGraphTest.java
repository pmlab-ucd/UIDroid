/*
 * https://groups.google.com/forum/#!topic/soot-list/bob7WNBJwwU
 */

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
	}

	public static void main(String[] args) {
		SetupApplication app = new SetupApplication(
				"C:/Users/hao/Downloads/android-sdk-windows/platforms",
				"F:/PATDroid/PATDroidTest/app/app-debug.apk");

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

		Options.v().set_process_dir(
				Collections.singletonList("F:/PATDroid/PATDroidTest/app/app-debug.apk"));

		Options.v()
				.set_android_jars(
						"C:/Users/hao/Downloads/android-sdk-windows/platforms");

		Options.v().set_whole_program(true);

		Options.v().set_allow_phantom_refs(true);
		// no output or produces a Dex/APK file as output
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