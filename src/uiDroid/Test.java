package uiDroid;

import java.io.IOException;


public class Test {
	public static void main(String[] args) {
		try {
			HandleResult.updateCG("sd");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void setView(int id) {
		System.out.print(id);
	}
}
