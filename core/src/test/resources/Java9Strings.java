public class Java9Strings {
	public static void main(String[] args) {
		int argCount = args.length;
		if (argCount < 3) {
			System.err.println("Usage: <protocol> <domain> [suffix]\n" +
					" - Expected 3 args, found " + argCount + " args");
			return;
		}
		String protocol = args[0];
		String domain = args[1];
		String suffix = args[2];
		System.out.println(protocol + "://" + domain + "/" + suffix);
	}
}