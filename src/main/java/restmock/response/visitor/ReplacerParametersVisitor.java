package restmock.response.visitor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import restmock.response.Response;

public class ReplacerParametersVisitor implements Visitor<Response> {

	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private final Map<String, String> parameters;

	public ReplacerParametersVisitor(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public void visit(Response response) {
		String input = response.getContent();
		Matcher matcher = PARAMETER_PATTERN.matcher(input);
		StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			String replacement = parameters.getOrDefault(key, matcher.group(0));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);

		response.setContent(sb.toString());
	}

}
