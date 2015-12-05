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

import au.com.bytecode.opencsv.CSVWriter;
import playGator.DefaultGUIAnalysisOutput;
import playGator.FixpointSolver;
import playGator.GUIAnalysis;
import playGator.Main;
import playGator.Config;
import presto.android.gui.graph.NNode;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.graph.NOpNode;
import presto.android.gui.listener.EventType;
import presto.android.xml.AndroidView;
import soot.SootMethod;

public class Test {

	private static String tag = "[Main]";

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
			// decompile
			String decomPath = args[0] + File.separator + "Decompiled"
					+ File.separator + fileName;
			Main.parseArgs(new String[1]);
			GUIAnalysis ga = GUIAnalysis.v();
			ga.run();
			FixpointSolver solver = ga.fixpointSolver;
			DefaultGUIAnalysisOutput output = ga.output;

			PermissionAnalysis pa = PermissionAnalysis.v();
			pa.run();
			// System.out.println(PermissionAnalysis.sensEntries);
			Map<String, AndroidView> views = getViews(ga, solver, output);
			try {
				writeCSV(views);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * run apktool to decompile the apk
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
			System.out.println(line);
		}
	}

	public static Map<String, AndroidView> getViews(GUIAnalysis ga,
			FixpointSolver solver, DefaultGUIAnalysisOutput output) {
		Map<String, AndroidView> map = new HashMap<>();
		ga.retrieveIds();
		for (Entry<NOpNode, Set<NNode>> entry : solver.solutionResults
				.entrySet()) {
			for (NNode node : entry.getValue()) {
				// System.out.print(tag + entry + ":: ");
				System.out.println(node);
				// System.out.println(output.getExplicitEventsAndTheirHandlers((NObjectNode)
				// node));

				for (Entry<EventType, Set<SootMethod>> et : output
						.getExplicitEventsAndTheirHandlers((NObjectNode) node)
						.entrySet()) {
					for (SootMethod method : et.getValue()) {
						// System.out.println(node.idNode.getIdValue());
						map.put(method.getSignature(), ga.xmlParser
								.findViewById(node.idNode.getIdValue()));
					}
				}
			}
		}

		return map;
	}

	/*
	 * write results to csv
	 */
	public static void writeCSV(Map<String, AndroidView> views)
			throws IOException {
		// String csv = "./sootOutput/data.csv";
		String csv = "./sootOutput/" + Config.benchmarkName + ".csv";
		String apkName = Config.benchmarkName;
		File csvFile = new File(csv);
		System.out.println(csv);
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
				System.out.println(tag + entry + ":: ");

				result.add(apkName);
				result.add(sens.getSignature());

				if (views.containsKey(entry.getSignature())) {
					System.out.println(views.get(entry.getSignature())
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

}
