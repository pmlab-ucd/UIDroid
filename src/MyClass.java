
public class MyClass {
	private void myMethod() {
		int x, a, b;
		x = 30;
		a = x - 1;
		b = x - 2;
		while (x > 0) {
			System.out.print(a * b - x);
			x = x - 1;
		}
		System.out.print(a * b);
	}

}
