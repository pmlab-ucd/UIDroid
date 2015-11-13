package uiDroid;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class HandleResultTest {
	private Map<String, String> strings;
	private Map<String, Widget> widgets;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		String xmlPath = "/home/hao/workspace/AppContext/Manifest/Decomplied/ApkSamples/app-debug.apk/res/values/strings.xml";
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseXML parser = new ParseStringsXML();
		strings = parser.parseXML(isXml, "utf-8");
		
		xmlPath = "/home/hao/workspace/AppContext/Manifest/Decomplied/ApkSamples/app-debug.apk/res/layout/activity_activity1.xml";
		xmlFile = new File(xmlPath);
		isXml = new FileInputStream(xmlFile);
		parser = new ParseLayoutXML();
		widgets = parser.parseXML(isXml, "utf-8");
	}

	@Test
	public void testUpdateCG() {
		/*try {
			HandleResult.updateCG("./sootOutput/app-debug.dot");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	@Test
	public void testUpdateUIStrs() throws FileNotFoundException {
		HandleResult.updateUIStr(widgets, strings);
		for (Widget widget : widgets.values()) {
			System.out.println(widget.getSid() + ": " + widget.getText());
		}
	}

}
