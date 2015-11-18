package playGator;

import static org.junit.Assert.*;
import presto.android.Configs;
import sun.security.krb5.Config;

import org.junit.Before;
import org.junit.Test;

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
	}

	@Test
	public void testRun() {
	}

}
