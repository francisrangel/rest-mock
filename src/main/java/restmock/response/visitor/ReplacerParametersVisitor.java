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

		while (matcher.find())
			input = replaceWildTags(input, matcher);

		response.setContent(input);
	}

	private String replaceWildTags(String input, Matcher matcher) {
		String expressionName = matcher.group(1);
		String expression = String.format("\\$\\{%s\\}", expressionName);
		String parameterValue = parameters.get(expressionName);

		if (parameterValue != null)
			input = input.replaceAll(expression, parameterValue);

		return input;
	}

}
