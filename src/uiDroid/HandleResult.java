package uiDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class HandleResult {	
	
	static List<WidgetResult> widgetResult;
	
	HandleResult(String apkPath, List<WidgetResult> widgetResult) {
		this.widgetResult = widgetResult;
		String[] args = new String[1];
		args[0] = apkPath;
		//CallFlowGraphSimplify.main(args);
		try {
			updateCG("./sootOutput/app-debug.dot");
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
					|| line.contains("void finalize")) {
				continue;
			}
			out.println(line);
			System.out.println(line);
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

}
