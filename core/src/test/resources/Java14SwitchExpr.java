public class Java14SwitchExpr {
	public static void main(String[] args) {
		System.out.println("Monday:    " + numLetters(Day.MONDAY));
		System.out.println("Tuesday:   " + numLetters(Day.TUESDAY));
		System.out.println("Wednesday: " + numLetters(Day.WEDNESDAY));
	}

	static int numLetters(Day day) {
		return switch (day) {
			case MONDAY, FRIDAY, SUNDAY -> 6;
			case TUESDAY                -> 7;
			default      -> {
				String s = day.toString();
				int result = s.length();
				yield result;
			}
		};
	}
}