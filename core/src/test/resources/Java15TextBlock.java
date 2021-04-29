public class Java15TextBlock {
	private static String html = """
            <html>
                <body>
                    <p>Hello, {}</p>
                </body>
            </html>
            """;

	public static void main(String[] args) {
		String name = args.length > 0 ? args[0] : "Person";
		System.out.println(html.replace("{}", name));
	}
}