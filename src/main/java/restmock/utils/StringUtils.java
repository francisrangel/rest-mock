package restmock.utils;

public class StringUtils {
	
	public static String uncapitalize(String word) {
		if (word == null) return null;
		if (word == "") return word;
		
		String[] words = word.split(" ");
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < words.length; i++) {
			sb.append(uncapitalizeWord(words[i]));
			if (i != words.length - 1) sb.append(" ");
		}
		
		return sb.toString();
	}
	
	private static String uncapitalizeWord(String word) {
		char firstCharAsLower = Character.toLowerCase(word.charAt(0));
		String withoutFirstChar = word.substring(1, word.length());
		return firstCharAsLower + withoutFirstChar;
	}

}
