package pullUp;

public class Main {

	public static void printResults(A a, B b) {
		System.out.println("Called from A: " + a.function() + "\n" + "Called from B: " + b.function());
	}

	public static void main(String[] args) {

		A classA = new A();
		B classB = new B();
		printResults(classA, classB);

	}

}
