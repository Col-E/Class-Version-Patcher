public record Java16Record(String example, int intArg1, int intArg2) {
	public boolean isArg1Greater() {
		return intArg1() > intArg2();
	}

	public static void main(String[] args) {
		Java16Record r1 = new Java16Record("Test", 1, 2);
		System.out.println("1 > 2 = " + r1.isArg1Greater());

		Java16Record r2 = new Java16Record("Test", 4, 3);
		System.out.println("4 > 3 = " + r2.isArg1Greater());
	}
}