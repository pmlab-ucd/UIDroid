package uiDroid;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ParseLayoutXMLTest {

	@Test
	public void testParseXML() {
		String xmlPath = "/home/hao/workspace/AppContext/Manifest/Decomplied/ApkSamples/app-debug.apk/res/layout/activity_activity1.xml";
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = null;
		try {
			isXml = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ParseXML parser = new ParseLayoutXML();
		Map<String, Widget> widgets = parser.parseXML(isXml, "utf-8");
		System.out.println(widgets);
		for (String str : widgets.keySet()) {
			System.out.println(str + ": " + widgets.get(str).getText());
		}
	}

}
