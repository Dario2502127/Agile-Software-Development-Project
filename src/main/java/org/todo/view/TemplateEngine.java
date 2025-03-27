package org.todo.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Logger;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class TemplateEngine {

	private static final String TEMPLATE_ROOT = "/templates/";
	private static final Logger LOGGER = Logger.getLogger(TemplateEngine.class.getName());

	public static void process(String templateName, HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream template = TemplateEngine.class.getResourceAsStream(TEMPLATE_ROOT + templateName);
		if (template == null) {
			LOGGER.severe("Loading template '" + TEMPLATE_ROOT + templateName + "' failed.");
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("<h2>500 Internal Server Error</h2>Loading template '" + TEMPLATE_ROOT + templateName + "' failed.");
			return;
		}
		String content = new String(template.readAllBytes(), StandardCharsets.UTF_8);
		for (String name : Collections.list(request.getAttributeNames())) {
			Object value = request.getAttribute(name);
			if (value != null) {
				String replacement = value instanceof Iterable ? renderIterable((Iterable) value) : value.toString();
				content = content.replaceAll("\\{" + name + "}", replacement);
			}
		}
		content = content.replaceAll("\\{.*?}", "");

		response.setContentType("text/html");
		response.getWriter().write(content);
	}

	private static String renderIterable(Iterable<Object> iterable) {
		StringBuilder builder = new StringBuilder();
		for (Object item : iterable) {
			builder.append("<li>").append(item).append("</li>");
		}
		if (builder.isEmpty()) {
			builder.append("<li>[Empty list]</li>");
		}
		return "<ul>" + builder + "</ul>";
	}
}
