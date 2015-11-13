package uiDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class HandleResult {	
	
	static List<WidgetResult> widgetResult;
	
	public static void storeResult(String cgPath, List<WidgetResult> widgetRes) 
			throws IOException {
		updateCG(cgPath, widgetRes);
		String apk = cgPath.split(".dot")[0];
		writeCSV(apk);
	}
	
	public static void updateCG(String cgPath, List<WidgetResult> widgetRes) {
		widgetResult = widgetRes;
		try {
			updateCG(cgPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void updateCG(String cgFilePath) throws IOException {
		File resultFile = new File(cgFilePath.split(".dot")[0] + ("_UI.dot"));
		PrintWriter out = null;
		List<String> onClicks = new ArrayList<>();
		try {
			out = new PrintWriter(resultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		File oldCG = new File(cgFilePath);
		BufferedReader br = new BufferedReader(new FileReader(oldCG));
		 
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains("dummy") && line.contains("onClick")) {
				onClicks.add(line.split("->")[1].split(";")[0]);
				continue;
			}
			if (line.contains("}") || line.contains("java.lang.Object: void registerNatives()")
					|| line.contains("void <clinit>")
					|| line.contains("void <init>")
					|| line.contains("void finalize")
					|| line.contains("findViewBy")) {
				continue;
			}
			out.println(line);
			//System.out.println(line);
		}
		br.close();
		
		Map<String, AbstractResource> widgets = new HashMap<>();
		for (WidgetResult wid : widgetResult) {
			widgets.put("\"" + wid.eventHandler.toString() + "\"", wid.widget);
		}
		
		for (String onClick : onClicks) {
			if (widgets.containsKey(onClick)) {
				out.println("    " + onClick.split("\\$")[0] + 
						": void onCreate(android.os.Bundle)>\"->\"" + widgets.get(onClick).getResourceName() + "\";");
				out.println("\"" + widgets.get(onClick).getResourceName() + "\"->" + onClick + ";");
			} else {
			out.println("    " + onClick.split("\\$")[0] + 
					": void onCreate(android.os.Bundle)>\"->" + onClick + ";");
			}
		}
		out.println("}");
		out.close();
	}
	
	public static void writeCSV(String apk) throws IOException {
		String csv = apk + ".csv";
		String[] tmp = apk.split("/");
		String apkName = tmp[tmp.length - 1];
		File csvFile = new File(csv);
		System.out.println(csv);
		if (!csvFile.exists()) {
			csvFile.createNewFile();
		}
		CSVWriter writer = new CSVWriter(new FileWriter(csv, true));
		List<String[]> results = new ArrayList<>();
		for (WidgetResult res : widgetResult) {
			List<String> result = new ArrayList<>();
			result.add(apkName);
			result.add(res.sensitive.toString());
			result.add(res.eventHandler.toString());
			if (res.widget != null) {
				result.add(res.widget.getResourceName());
			} else {
				result.add("");
			}
			String[] resultArray = (String[]) result
					.toArray(new String[result.size()]);
			results.add(resultArray);
		}
		
		writer.writeAll(results);
		writer.close();
	}

}
