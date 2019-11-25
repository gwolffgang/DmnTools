package de.materna.dmntools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DmnModelElementInstance;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

public abstract class DmnUtils {

	public static int countCharacter(final String str, final char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}

	public static int countCharacter(final String str, final String chars) {
		int count = 0;
		final Set<Character> uniqueChars = new LinkedHashSet<>();
		for (final char c : chars.toCharArray()) {
			uniqueChars.add(c);
		}
		for (final char c : uniqueChars) {
			count += DmnUtils.countCharacter(str, c);
		}
		return count;
	}

	public static String getDecisionTableType(final DecisionTable table) {
		if (table.getOutputs() == null) {
			return "";
		}
		return (table.getOutputs().size() == 1 ? "1" : "n")
				.concat(hasSingleResult(table) ? "1" : "n");
	}

	public static List<File> getDmnFilesList(final File dir) {
		return Arrays.asList(dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String filename) {
				return filename.endsWith(".dmn");
			}
		}));
	}

	public static String getInputVariableType(final Input input) {
		final String camundaInputVariable = input.getCamundaInputVariable();
		final String inputExpression = input.getInputExpression().getTextContent();
		final boolean isNamed = DmnUtils.isFeelName(camundaInputVariable);
		final boolean isVariable = DmnUtils.isFeelName(inputExpression);
		final boolean isEmpty = inputExpression.isEmpty();
		return (isNamed ? "named " : "unnamed ")
				.concat(isVariable ? "variable" : (isEmpty ? "empty" : "formula"));
	}

	public static String getJavaVariableClass(final String typeRef) {
		if (typeRef.equals("date")) {
			return "LocalDateTime";
		}
		return typeRef.substring(0, 1).toUpperCase() + typeRef.substring(1, typeRef.length());
	}

	public static String getJavaVariableType(final String typeRef) {
		final String shortTypeRef = typeRef
				.substring(typeRef.indexOf(":") == -1 ? 0 : typeRef.indexOf(":") + 1);
		switch (shortTypeRef) {
		case "date":
			return "LocalDateTime";
		case "integer":
			return "int";
		case "number":
			return "double";
		case "string":
			return "String";
		default:
			return shortTypeRef;
		}
	}

	public static boolean hasSingleResult(final DecisionTable table) {
		return !((table.getHitPolicy() == HitPolicy.RULE_ORDER)
				|| (table.getHitPolicy() == HitPolicy.OUTPUT_ORDER)
				|| ((table.getHitPolicy() == HitPolicy.COLLECT)
						&& (table.getAggregation() == null)));
	}

	public static boolean isChildOf(final ModelElementInstance child,
			final Class<?> parentClassType) {
		return child.getParentElement().getClass() == parentClassType;
	}

	public static boolean isDmnElementType(final DmnModelElementInstance dmnModelElementInstance,
			final Class<?> classType) {
		if (dmnModelElementInstance == null) {
			return false;
		}
		return dmnModelElementInstance.getClass() == classType;
	}

	public static boolean isFeelName(final String string) {
		if (string != null) {
			final Matcher matcher = QUALIFIED_NAME().matcher(string);
			return (matcher.matches() && !string.matches("(null|true|false|\\-)"));
		}
		return false;
	}

	public static boolean isRange(final String string) {
		return (string.startsWith("[") || string.startsWith("]") || string.startsWith("(")
				|| string.startsWith(")"))
				&& (string.endsWith("[") || string.endsWith("]") || string.endsWith("(")
						|| string.endsWith(")"))
				&& string.contains("..") && !string.contains("...");
	}

	public static String namingConvention(final String name, final String type) {
		final String[] parts = name.split("[/\\- ]");
		final StringBuilder reducedName = new StringBuilder();
		reducedName.append(parts[0]);
		for (int partNr = 1; partNr < parts.length; partNr++) {
			reducedName.append(parts[partNr].substring(0, 1).toUpperCase())
					.append(parts[partNr].substring(1));
		}
		final StringBuilder finalName = new StringBuilder();
		if (reducedName.length() > 0) {
			switch (type) {
			case "class":
				finalName.append(reducedName.substring(0, 1).matches("[A-Za-z]") ? "" : "Dmn");
			case "intern":
				finalName.append(reducedName.toString().toUpperCase().charAt(0));
				break;
			case "method":
			case "variable":
				finalName.append(reducedName.toString().toLowerCase().charAt(0));
				break;
			case "unchanged":
				finalName.append(reducedName.charAt(0));
			}
			finalName.append(reducedName.substring(1));
		}
		return finalName.toString().replaceAll("[-#]", "");
	}

	public static Pattern QUALIFIED_NAME() {
		final Pattern ADDITIONAL_NAME_SYMBOLS = Pattern.compile("[\\./\\-’'+*]");
		final Pattern NAME_START_CHAR = Pattern
				.compile("[?A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF"
						.concat("\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F")
						.concat("\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD")
						.concat("\\u10000-\\uEFFFF]"));
		final Pattern NAME_PART_CHAR = Pattern.compile("(".concat(NAME_START_CHAR.pattern())
				.concat("|[0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040])"));
		final Pattern NAME_PART = Pattern
				.compile(NAME_PART_CHAR.pattern().concat(NAME_PART_CHAR.pattern()).concat("+"));
		final Pattern NAME_START = Pattern.compile(
				NAME_START_CHAR.pattern().concat("").concat(NAME_PART_CHAR.pattern()).concat("+"));
		final Pattern NAME = Pattern
				.compile(NAME_START.pattern().concat("(").concat(NAME_PART.pattern()).concat("|")
						.concat(ADDITIONAL_NAME_SYMBOLS.pattern()).concat(")+"));
		final Pattern QUALIFIED_NAME = Pattern.compile(NAME + "+([.](" + NAME + "+){1})*");
		return QUALIFIED_NAME;
	}

	public static ArrayList<String> splitMatch(final String match) {
		final String[] parts = match.split(",");
		final ArrayList<String> list = new ArrayList<>();
		for (int part = 0; part < parts.length; part++) {
			int neighbor = 0;
			String result = parts[part];
			while (((DmnUtils.countCharacter(result, '"') % 2) == 1)
					|| ((DmnUtils.countCharacter(result, "()[]") % 2) == 1)) {
				result = result.concat(",").concat(parts[part + ++neighbor]);
			}
			list.add(result.trim());
			part += neighbor;
		}
		return list;
	}
}
