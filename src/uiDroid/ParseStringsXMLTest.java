package uiDroid;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.junit.Test;

public class ParseStringsXMLTest {

	@Test
	public void testParseXML() throws FileNotFoundException {
		String xmlPath = "/home/hao/workspace/AppContext/Manifest/Decomplied/ApkSamples/app-debug.apk/res/values/strings.xml";
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		ParseXML parser = new ParseStringsXML();
		Map<String, String> strings = parser.parseXML(isXml, "utf-8");
		for (String string : strings.keySet()) {
			System.out.println(string + ": " + strings.get(string));
		}
	}

}
