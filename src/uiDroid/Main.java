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
			Config.sdkDir = "/home/hao/Android/Sdk";
			Config.apkPath = args[0] + File.separator + fileName;
			String jarPath = Scene.v().getAndroidJarPath(Config.sdkDir + "/platforms", Config.apkPath);
			Config.apiLevel = "android-" + jarPath.split("/android.jar")[0].split("-")[1];
			// decompile
			String decomPath = args[0] + File.separator + "Decompiled"
					+ File.separator + fileName;
			decompile(decomPath);
			Config.project = decomPath;
			
			Config.android = "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel + "/framework.jar:"
					+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel +"/bouncycastle.jar:"
					+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel + "/ext.jar:"
					+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel + "/android-policy.jar:"
					+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel + "/services.jar:"
					+ jarPath
					+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/annotations.jar:"
					+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/android-support-v4.jar:";
			Config.jre = "/home/hao/workspace/gator-3.0/AndroidBench/platform/" + Config.apiLevel + "/core.jar";
			Config.benchmarkName = fileName;
			Config.listenerSpecFile = "/home/hao/workspace/gator-3.0/SootAndroid/listeners.xml";
			Config.wtgSpecFile = "/home/hao/workspace/gator-3.0/SootAndroid/wtg.xml";
			Config.guiAnalysis = true;
			Config.apkMode = false;
			Config.processing();
			
			GUIAnalysis ga = GUIAnalysis.v();
			FixpointSolver solver = ga.fixpointSolver;
			DefaultGUIAnalysisOutput output = ga.output;
			// FIXME
			//Map<String, AndroidView> views = getViews(ga, solver, output);
			
			//PermissionAnalysis pa = PermissionAnalysis.v();
			PerInvCtxAnalysis pa = PerInvCtxAnalysis.v();
			pa.run();
			// print(PermissionAnalysis.sensEntries);	
			/*try {
				writeCSV(views);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
	}

	/** 
	 * @Title: decompile 
	 * @Description:  Run apktool to decompile the apk
	 * @param decomPath
	 * @throws IOException
	 * @throws InterruptedException    
	 * @return: void    
	 */
	public static void decompile(String decomPath) throws IOException,
			InterruptedException {
		String cmd = "apktool d " + Config.apkPath + " -o " + decomPath;
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);
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
		
		for (Entry<NOpNode, Set<NNode>> entry : solver.solutionResults
				.entrySet()) {
			// print("Operation: " + entry + ":: ");
			for (NNode node : entry.getValue()) {
				if (ga.xmlParser
							.findViewById(node.idNode.getIdValue()) != null) {
					print("Operation: " + entry + ":: ");
				 print(ga.xmlParser
							.findViewById(node.idNode.getIdValue()).getText());
				 print(output
						.getExplicitEventsAndTheirHandlers((NObjectNode) node)
						.entrySet().toString());
				}
				 //print(output.getExplicitEventsAndTheirHandlers((NObjectNode)
				 //node).toString());
				for (Entry<EventType, Set<SootMethod>> et : output
						.getExplicitEventsAndTheirHandlers((NObjectNode) node)
						.entrySet()) {
					for (SootMethod method : et.getValue()) {
						//print(node.idNode.getIdValue().toString());
						map.put(method.getSignature(), ga.xmlParser
								.findViewById(node.idNode.getIdValue()));
						print(method.getSignature());
						print(ga.xmlParser
								.findViewById(node.idNode.getIdValue()).getText());
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
		for (SootMethod sens : PermissionAnalysis.sensEntries.keySet()) {
			for (SootMethod entry : PermissionAnalysis.sensEntries.get(sens)) {
				List<String> result = new ArrayList<>();
				print(entry.toString() + ":: ");

				result.add(apkName);
				result.add(sens.getSignature());

				if (views.containsKey(entry.getSignature())) {
					print(views.get(entry.getSignature())
							.getText());
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
