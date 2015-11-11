package uiDroid;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class HandleResultTest {

	@Test
	public void testUpdateCG() {
		try {
			HandleResult.updateCG("./sootOutput/app-debugSimple.dot");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
