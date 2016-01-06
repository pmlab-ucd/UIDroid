package uiDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.opencsv.CSVWriter;

import playAppContext.Context;
import playAppContext.PermissionInvocation;
import playGator.DefaultGUIAnalysisOutput;
import playGator.FixpointSolver;
import playGator.GUIAnalysis;
import playGator.Config;
import presto.android.gui.graph.NNode;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.graph.NOpNode;
import presto.android.gui.listener.EventType;
import presto.android.xml.AndroidView;
import soot.Scene;
import soot.SootMethod;

public class Main {

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

		for (final String fileName : apkFiles) {
			print("Begin to analyze: " + fileName);
			Config.apkPath = args[0] + File.separator + fileName;
			// decompile
			String decomPath = args[0] + File.separator + "Decompiled"
					+ File.separator + fileName;
			decompile(decomPath);
			Config.project = decomPath;
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("linux") >= 0) {
				Config.sdkDir = "/home/hao/Android/Sdk";
				String jarPath = Scene.v().getAndroidJarPath(
						Config.sdkDir + "/platforms", Config.apkPath);
				Config.apiLevel = "android-"
						+ jarPath.split("/android.jar")[0].split("-")[1];
				Config.android = "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel
						+ "/framework.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel
						+ "/bouncycastle.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel
						+ "/ext.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel
						+ "/android-policy.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel
						+ "/services.jar:"
						+ jarPath
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/annotations.jar:"
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/android-support-v4.jar:";
				Config.jre = "/home/hao/workspace/gator-3.0/AndroidBench/platform/"
						+ Config.apiLevel + "/core.jar";
				Config.listenerSpecFile = "/home/hao/workspace/gator-3.0/SootAndroid/listeners.xml";
				Config.wtgSpecFile = "/home/hao/workspace/gator-3.0/SootAndroid/wtg.xml";
			} else {
				Config.sdkDir = "C:/Users/hao/Downloads/android-sdk-windows/";
				String jarPath = Scene.v().getAndroidJarPath(
						Config.sdkDir + "/platforms", Config.apkPath);
				print("jarPath: " + jarPath);
				String[] tmp = jarPath.split("\\\\android.jar")[0]
						.split("android-");
				Config.apiLevel = "android-" + tmp[tmp.length - 1];
				Config.android = "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel
						+ "/framework.jar:"
						+ "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel
						+ "/bouncycastle.jar:"
						+ "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel
						+ "/ext.jar:"
						+ "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel
						+ "/android-policy.jar:"
						+ "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel
						+ "/services.jar:"
						+ jarPath
						+ "C:/Users/hao/workspace/Gator/gator/deps/annotations.jar:"
						+ "C:/Users/hao/workspace/Gator/gator/deps/android-support-v4.jar:";
				Config.jre = "C:/Users/hao/workspace/Gator/gator/lib/AndroidBench"
						+ Config.apiLevel + "/core.jar";
				Config.listenerSpecFile = "C:/Users/hao/workspace/Gator/gator/listeners.xml";
				Config.wtgSpecFile = "C:/Users/hao/workspace/Gator/gator/wtg.xml";
			}

			Config.benchmarkName = fileName;
			Config.guiAnalysis = true;
			Config.apkMode = false;
			Config.processing();

			GUIAnalysis ga = GUIAnalysis.v();
			ga.run();
			FixpointSolver solver = ga.fixpointSolver;
			DefaultGUIAnalysisOutput output = ga.output;
			// FIXME
			Map<String, AndroidView> views = getViews(ga, solver, output);

			// PermissionAnalysis pa = PermissionAnalysis.v();
			/*PerInvCtxAnalysis pa = PerInvCtxAnalysis.v();
			pa.run();

			try {
				writeCSV(views);
			} catch (IOException e) {
				e.printStackTrace();
			}*/

		}
	}

	/**
	 * @Title: decompile
	 * @Description: Run apktool to decompile the apk
	 * @param decomPath
	 * @throws IOException
	 * @throws InterruptedException
	 * @return: void
	 */
	public static void decompile(String decomPath) throws IOException,
			InterruptedException {
		String OS = System.getProperty("os.name").toLowerCase();
		print(OS);
		String cmd = " d " + Config.apkPath + " -o " + decomPath;
		Process pr;
		if (OS.indexOf("linux") >= 0) {
			pr = Runtime.getRuntime().exec("apktool " + cmd);
		} else {
			pr = Runtime.getRuntime()
					.exec("java -jar -Duser.language=en C:/Windows/apktool.jar"
							+ cmd);
		}

		pr.waitFor();
		BufferedReader buf = new BufferedReader(new InputStreamReader(
				pr.getInputStream()));
		String line = "";
		while ((line = buf.readLine()) != null) {
			print(line);
		}
	}

	/**
	 * @Title: getViews
	 * @Description: Get Views: (event handler callback : view)
	 * @param ga
	 * @param solver
	 * @param output
	 * @return: Map<String,AndroidView>
	 */
	public static Map<String, AndroidView> getViews(GUIAnalysis ga,
			FixpointSolver solver, DefaultGUIAnalysisOutput output) {
		Map<String, AndroidView> map = new HashMap<>();
		ga.retrieveIds();

		print("solver: " + solver);
		print("solution: " + solver.solutionResults);
		for (Entry<NOpNode, Set<NNode>> entry : solver.solutionResults
				.entrySet()) {
			// print("Operation: " + entry + ":: ");
			for (NNode node : entry.getValue()) {
				if (ga.xmlParser.findViewById(node.idNode.getIdValue()) != null) {
					print("Operation: " + entry + ":: ");
					print(ga.xmlParser.findViewById(node.idNode.getIdValue())
							.getText());
					print("event handler: "
							+ output.getExplicitEventsAndTheirHandlers(
									(NObjectNode) node).entrySet().toString());
				}
				// print(output.getExplicitEventsAndTheirHandlers((NObjectNode)
				// node).toString());
				for (Entry<EventType, Set<SootMethod>> et : output
						.getExplicitEventsAndTheirHandlers((NObjectNode) node)
						.entrySet()) {
					for (SootMethod method : et.getValue()) {
						// print(node.idNode.getIdValue().toString());
						map.put(method.getSignature(), ga.xmlParser
								.findViewById(node.idNode.getIdValue()));
						print(method.getSignature());
						print(ga.xmlParser.findViewById(
								node.idNode.getIdValue()).getText());
					}
				}
			}
		}

		return map;
	}

	/**
	 * @Title: writeCSV
	 * @Description: write results to csv
	 * @param views
	 * @throws IOException
	 * @return: void
	 */
	public static void writeCSV(Map<String, AndroidView> views)
			throws IOException {
		// String csv = "./sootOutput/data.csv";
		String csv = "./sootOutput/" + Config.benchmarkName + ".csv";
		String apkName = Config.benchmarkName;
		File csvFile = new File(csv);
		print(csv);
		if (!csvFile.exists()) {
			csvFile.createNewFile();
		} else {
			csvFile.delete();
			csvFile.createNewFile();
		}
		CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
		List<String[]> results = new ArrayList<>();
		// for (SootMethod sens : PermissionAnalysis.sensEntries.keySet()) {
		// for (SootMethod entry : PermissionAnalysis.sensEntries.get(sens)) {
		for (PermissionInvocation perInvoc : PerInvCtxAnalysis.perInvocs) {
			SootMethod sens = perInvoc.getTgt();
			for (Context ctx : perInvoc.getContexts()) {
				SootMethod entry = ctx.getEntrypoint();
				List<String> result = new ArrayList<>();
				print(entry.toString() + ":: ");

				result.add(apkName);
				result.add(sens.getSignature());

				if (views.containsKey(entry.getSignature())) {
					print(views.get(entry.getSignature()).getText());
					result.add(views.get(entry.getSignature()).getText());
				} else {
					result.add(" ");
				}
				String[] resultArray = (String[]) result
						.toArray(new String[result.size()]);
				results.add(resultArray);
			}
		}

		writer.writeAll(results);
		writer.close();
	}

	public static void print(String str) {
		System.err.println("[UIDroid] " + str);
	}

}
