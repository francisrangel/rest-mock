package restmock.response.visitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import restmock.response.Response;

public class ReplacerParametersVisitor implements Visitor<Response> {
	
	private final HttpServletRequest request;
	
	public ReplacerParametersVisitor(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public void visit(Response response) {
		String regex = "\\$\\{(.+?)\\}";
		
		String input = response.getContent();
		Matcher matcher = Pattern.compile(regex).matcher(input);
		
		while (matcher.find()) 
			input = replaceWildTags(input, matcher);
		
		response.setContent(input);
	}

	private String replaceWildTags(String input, Matcher matcher) {
		String expressionName = matcher.group(1);
		String expression = String.format("\\$\\{%s\\}", expressionName);
		String parameterValue = request.getParameter(expressionName);
		
		if (parameterValue != null)
			input = input.replaceAll(expression, parameterValue);
		
		return input;
	}

}
