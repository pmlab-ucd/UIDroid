package playGator;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import presto.android.xml.AndroidView;

public class GUIAnalysisTest {
	GUIAnalysis ga;
	
	@Before 
	public void setup() {
		Main.parseArgs(new String[1]);
		ga = GUIAnalysis.v();
	}
	
	@Test
	public void testV() {	
		assertNotEquals(ga, null);
	}

	@Test
	public void testGUIAnalysis() {
	}

	@Test
	public void testRetrieveIds() {
		ga.retrieveIds();
		AndroidView view = ga.xmlParser.findViewById(2131230720);
		assertNotEquals(view, null);
		System.out.println("text:" + view.getText());
	}

	@Test
	public void testRun() {
		//ga.run();
	}

}
