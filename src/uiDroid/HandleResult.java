package uiDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class HandleResult {	
	
	private static List<WidgetResult> widgetResult;
	
	/*
	 * main procedure
	 */
	public static void storeResult(String cgPath, List<WidgetResult> widgetRes,
			String decomPath) throws IOException {
		updateCG(cgPath, widgetRes);
		String apk = cgPath.split(".dot")[0];
		
		Map<String, String> strings;
		Map<String, Widget> widgets = new HashMap<>();
		String xmlPath = decomPath + "/res/values/strings.xml";
		strings = getStrPool(xmlPath);
		
		List<String> layoutXmls = getAllLayoutXmls(decomPath + "/res/layout/");
		for (String xml : layoutXmls) {
			xmlPath = decomPath + "/res/layout/" + xml;
			widgets.putAll(getWidgets(xmlPath));
		}		
		updateUIStr(widgets, strings);
		writeCSV(apk, widgets);
	}
	
	public static void updateCG(String cgPath, List<WidgetResult> widgetRes) {
		widgetResult = widgetRes;
		try {
			updateCG(cgPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * retrieve all strings declared in strings.xml
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getStrPool(String xmlPath) throws FileNotFoundException {
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseXML parser = new ParseStringsXML();
		return parser.parseXML(isXml, "utf-8");
	}
	
	/*
	 * retrieve all widgets declared in the layout xml
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Widget> getWidgets(String xmlPath) throws FileNotFoundException {
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseXML parser = new ParseLayoutXML();
		return parser.parseXML(isXml, "utf-8");
	}
	
	public static List<String> getAllLayoutXmls(String layoutPath) {
		File file = new File(layoutPath);
		List<String> xmls = new ArrayList<>();
		if (file.isDirectory()) {
			String[] dirFiles = file.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".xml"));
				}

			});
			for (String s : dirFiles)
				xmls.add(s);
		} 
		
		return xmls;
	}
	
	/*
	 * update call graph with UI info
	 */
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
		
		Map<String, WidgetResult> widgets = new HashMap<>();
		for (WidgetResult wid : widgetResult) {
			widgets.put("\"" + wid.eventHandler + "\"", wid);
		}
		 
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains("dummy") && line.contains("onClick")) {
				String onClick = line.split("->")[1].split(";")[0];
				if (widgets.containsKey(onClick)) {
					onClicks.add(onClick);
					continue;
				}
			}
			if (line.contains("}") || line.contains("java.lang.Object: void registerNatives()")
					|| line.contains("void <clinit>")
					|| line.contains("void <init>")
					|| line.contains("void finalize")
					|| line.contains("findViewBy")
					|| line.contains("setOnClickListener")) {
				continue;
			}
			out.println(line);
			//System.out.println(line);
		}
		br.close();
		

		/*
		for (String onClick : onClicks) {
			if (widgets.containsKey(onClick)) {
				out.println("    " + onClick.split("\\$")[0] + 
						": void onCreate(android.os.Bundle)>\"->\"" + widgets.get(onClick).getResourceName() + "\";");
				out.println("\"" + widgets.get(onClick).getResourceName() + "\"->" + onClick + ";");
			} else {
			out.println("    " + onClick.split("\\$")[0] + 
					": void onCreate(android.os.Bundle)>\"->" + onClick + ";");
			}
		}*/
		for (String onClick : onClicks) {
			out.println("    \"" + widgets.get(onClick).onCreate + 
					"\"->\"" + widgets.get(onClick).widget.getResourceName() + "\";");
			out.println("    \"" + widgets.get(onClick).widget.getResourceName() + "\"->" + onClick + ";");
		}
		out.println("}");
		out.close();
	}
	
	/*
	 * write results to csv
	 */
	public static void writeCSV(String apk, Map<String, Widget> widgets) throws IOException {
		//String csv = apk + ".csv";
		String csv = "./sootOutput/data.csv";
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
				result.add(widgets.get("@id/" + res.widget.getResourceName()).getText());
			} else {
				result.add("");
			}
			String[] resultArray = (String[]) result
					.toArray(new String[result.size()]);
			if (!results.contains(resultArray)) {
				results.add(resultArray);
			}
		}
		
		writer.writeAll(results);
		writer.close();
	}
	
	/*
	 * update text who have @string/ to their real values
	 */
	public static void updateUIStr(Map<String, Widget> widgets, Map<String, String> strPool) {
		for (Widget widget : widgets.values()) {
			String text = widget.getText();
			if (text.contains("@string")) {
				text = text.split("@string/")[1];
				widget.setText(strPool.get(text));
			}
		}
	}

}
