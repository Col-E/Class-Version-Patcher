public class Java16InstanceOf {
	public static void main(String[] args) {
		printDetailed(100);
		printDetailed(100.5);
		printDetailed(100.005);
		printDetailed(100.000005);
		printDetailed("Hello");
	}

	static void printDetailed(Object value) {
		if (value instanceof Double d) {
			System.out.printf("Floating Point: %20f\n", d);
		} else if (value instanceof Integer i) {
			System.out.printf("Int:            %20d\n", i);
		} else if (value instanceof String s) {
			System.out.printf("Text:           %20s\n", s);
		}
	}
}