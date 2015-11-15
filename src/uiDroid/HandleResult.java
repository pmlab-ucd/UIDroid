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

public class HandleResult {	
	
	private static List<WidgetResult> widgetResult;
	
	/*
	 * main procedure
	 */
	public static void storeResult(String cgPath, List<WidgetResult> widgetRes,
			String decomPath,  List<String> eventHandlerTemps) throws IOException {
		updateCG(cgPath, widgetRes);
		String apk = cgPath.split(".dot")[0];
		
		Map<String, String> strings;
		Map<String, Widget> widgets = new HashMap<>();
		Map<String, Map<String, Widget>> activities = new HashMap<>();
		String xmlPath = decomPath + "/res/values/strings.xml";
		File xmlFile = new File(xmlPath);
		if (!xmlFile.isFile()) {
			xmlPath = decomPath + "/res/values-uk/strings.xml";
			xmlFile = new File(xmlPath);
		}
		strings = getStrPool(xmlFile);
		
		List<String> layoutXmls = getAllLayoutXmls(decomPath + "/res/layout/");
		for (String xml : layoutXmls) {
			xmlPath = decomPath + "/res/layout/" + xml;
			Map<String, Widget> nowWidgets = getWidgets(xmlPath, eventHandlerTemps);
			widgets.putAll(nowWidgets);
			activities.put(xml.split(".xml")[0], nowWidgets);
		}		
		
		xmlPath = decomPath + "/AndroidManifest.xml";
		Map<String, String> manifest = getActivities(xmlPath);
		
		updateUIStr(widgets, strings);
		writeCSV(apk, widgets, manifest, activities, strings);
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
	public static Map<String, String> getStrPool(File xmlFile) {
		FileInputStream isXml = null;
		try {
			isXml = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ParseXML parser = new ParseStringsXML();
		return parser.parseXML(isXml, "utf-8");
	}
	
	/*
	 * retrieve all widgets declared in the layout xml
	 */
	public static Map<String, Widget> getWidgets(String xmlPath,  List<String>  eventHandlerTemps) throws FileNotFoundException {
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseLayoutXML parser = new ParseLayoutXML();
		parser.setEventHandlerTemp(eventHandlerTemps);
		return parser.parseXML(isXml, "utf-8");
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> getActivities(String xmlPath) throws FileNotFoundException {
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseXML parser = new ParseManifestXML();
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
			if (widgets.get(onClick) != null && widgets.get(onClick).widget != null) {
				out.println("    \"" + widgets.get(onClick).onCreate + 
						"\"->\"" + widgets.get(onClick).widget.getResourceName() + "\";");
				out.println("    \"" + widgets.get(onClick).widget.getResourceName() + "\"->" + onClick + ";");
			}
		}
		out.println("}");
		out.close();
	}
	
	/*
	 * write results to csv
	 */
	public static void writeCSV(String apk, Map<String, Widget> widgets,
			Map<String, String> manifest, Map<String, Map<String, Widget>> activities,
			Map<String, String> strings) throws IOException {
		//String csv = "./sootOutput/data.csv";
		String csv = apk + ".csv";
		String[] tmp = apk.split("/");
		String apkName = tmp[tmp.length - 1];
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
		for (WidgetResult res : widgetResult) {
			List<String> result = new ArrayList<>();
			result.add(apkName);
			result.add(res.sensitive.toString());
			result.add(res.eventHandler.toString());
			if (res.widget != null) {
				result.add(res.widget.getResourceName());
				if (widgets.get("@id/" + res.widget.getResourceName()) == null) {
					result.add("");
				} else {
					result.add(widgets.get("@id/" + res.widget.getResourceName()).getText());
				}
			} else if (res.eventHandler.toString().contains("android.view.View")) {
				String activity = res.eventHandler.toString().split(": ")[0].split("<")[1];
				String mname = res.eventHandler.toString().split(": ")[1];
				String act = manifest.get(activity);
				Map<String, Widget> nowWidgets = activities.get(act);
				act = act.split("@string/")[1];
				act = strings.get(act);
				System.out.println(act);
				for (Widget widget : nowWidgets.values()) {
					for (String callback : widget.getCallback()) {
						if (mname.contains(callback)) {
							result.add(widget.getSid());
							result.add(widget.getText());
							break;
						}
					}
				}
			} else {
				result.add("");
			}
			String[] resultArray = (String[]) result
					.toArray(new String[result.size()]);
			//if (!results.contains(resultArray)) {
				results.add(resultArray);
			//}
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
			if (text != null && text.contains("@string")) {
				text = text.split("@string/")[1];
				widget.setText(strPool.get(text));
			}
		}
	}

}
