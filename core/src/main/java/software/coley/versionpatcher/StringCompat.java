package software.coley.versionpatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A class to substitute dynamic string concatenation.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unused")
public final class StringCompat {
	private static final char TAG_ARG = '\u0001';
	private static final char TAG_CONST = '\u0002';
	private static final String TAG_ARG_STR = String.valueOf(TAG_ARG);
	private final List<String> list = new ArrayList<String>();

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(boolean value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(byte value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(char value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(short value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(int value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(long value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(double value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(float value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(boolean[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(byte[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(char[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(short[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(int[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(long[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(double[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(float[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(Object[] value) {
		add(Arrays.toString(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to append to arguments.
	 *
	 * @return Self.
	 */
	public StringCompat insert(Object value) {
		add(String.valueOf(value));
		return this;
	}

	/**
	 * @param value
	 * 		Value to insert.
	 */
	private void add(String value) {
		list.add(0, value);
	}

	/**
	 * @param recipe
	 * 		Template.
	 *
	 * @return Filled in string with internally stored arguments.
	 */
	public String build(String recipe) {
		return compile(recipe, list);
	}

	/**
	 * @param recipe
	 * 		Template.
	 * @param arguments
	 * 		List of arguments to fill in.
	 *
	 * @return Filled in string.
	 */
	private static String compile(String recipe, List<String> arguments) {
		int c = 0;
		while (recipe.indexOf(TAG_ARG) >= 0) {
			if (c == arguments.size())
				return recipe;
			String arg = arguments.get(c);
			recipe = recipe.replaceFirst(TAG_ARG_STR, Matcher.quoteReplacement(arg));
			c++;
		}
		return recipe;
	}
}
