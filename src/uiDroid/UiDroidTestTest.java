package uiDroid;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;

import soot.PackManager;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.axml.*;

public class UiDroidTestTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testMainStringArray() throws ZipException, IOException {
		//UiDroidTest.main(null);
		ARSCFileParser fileParser = new ARSCFileParser();
		File file = new File(
				"/home/hao/workspace/AppContext/Instrument/InstrumentedApp/ApkSamples/app-debug.apk");
		String apkPath = file.getAbsolutePath();
		try {
			fileParser.parse(apkPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<Integer, String> strMap = fileParser.getGlobalStringPool();
		for (Integer i : strMap.keySet()) {
			System.out.print(i);
			System.out.println(": " + strMap.get(i));
		}
		

		ZipFile zip = new ZipFile(file);
		
		// search for file with given filename
		Enumeration<?> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String entryName = entry.getName();
			System.out.println(entryName);
			//if (entryName.equals(filename)) {
				//is = this.zip.getInputStream(entry);
				//break;
			//}
		}
		zip.close();
		
		ApkHandler apk = new ApkHandler(file);
		//InputStream is = apk.getInputStream("AndroidManifest.xml");
		InputStream is = apk.getInputStream("res/layout/activity_activity1.xml");
		
		String xmlPath = "/home/hao/workspace/AppContext/Manifest/Decomplied/ApkSamples/app-debug.apk/res/layout/activity_activity1.xml";
		File xmlFile = new File(xmlPath);
		FileInputStream isXml = new FileInputStream(xmlFile);
		List<Widget> widgets = LayoutXMLParser.parseXML(isXml, "utf-8");
		System.out.println(widgets);
		for (Widget widget : widgets) {
			System.out.println(widget.getSid() + ": " + widget.getText());
		}
	}

	@Test
	public void testMyTestMainStringArray() {
	}

	@Test
	public void testGetEntries() {
	}

	@Test
	public void testBfsCG() {
	}

	@Test
	public void testGetWidgets() {
	}

	@Test
	public void testGetAllOnCreate() {
	}

	@Test
	public void testAnalyzeOnCreate() {
	}

	@Test
	public void testExtractId() {
	}

	@Test
	public void testPermissionAnalysisStringStringString() {
	}

}
