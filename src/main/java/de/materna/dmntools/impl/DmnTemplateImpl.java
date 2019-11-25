package de.materna.dmntools.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputEntry;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnTemplate;
import de.materna.dmntools.DmnUtils;

public class DmnTemplateImpl implements DmnTemplate {

	private final List<String> code = new ArrayList<>();

	private final DmnMetaModel dmn;
	private final DmnEngine dmnEngine;

	public DmnTemplateImpl(final DmnMetaModel dmnData, final DmnEngine dmnEngine) {
		this.dmn = dmnData;
		this.dmnEngine = dmnEngine;
	}

	@Override
	public void addCamundaExecuteMethodSetVariable() {
		if ((this.dmn.getMainDecision() != null) && DmnUtils.isDmnElementType(
				this.dmn.getMainDecision().getExpression(), DecisionTableImpl.class)) {
			final List<Output> outputs = new ArrayList<>(
					((DecisionTable) this.dmn.getMainDecision().getExpression()).getOutputs());
			final String resultName = "result"
					.concat(DmnUtils.namingConvention(this.dmn.getDefinitions().getId(), "intern"));
			final String outputVariable = (outputs.size() == 1)
					&& (outputs.get(0).getName() != null) ? outputs.get(0).getName() : resultName;
			addLine("\t\texecution.setVariable(\"".concat(outputVariable)
					.concat("\", inputVariables.get(\"".concat(outputVariable).concat("\"));")));
		}
	}

	@Override
	public void addDecisionTableMethodFooter(final Decision decision) {
		addLine("\t\t}");
		final String mapName = DmnUtils.namingConvention(decision.getId(), "variable");
		if (DmnUtils.getDecisionTableType((DecisionTable) decision.getExpression()).contains("n")) {
			final String list = DmnUtils
					.getDecisionTableType((DecisionTable) decision.getExpression()).equals("nn")
							? "List"
							: "";
			addLine("\t\tinputVariables.put(\"".concat(mapName).concat(list).concat("\", ")
					.concat(mapName).concat(list).concat(");"));
		} else {
			final DecisionTable table = (DecisionTable) decision.getExpression();
			if ((table.getHitPolicy() == HitPolicy.COLLECT) && (table.getAggregation() != null)) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				final List<Output> outputs = new ArrayList(table.getOutputs());
				addLine("\t\tinputVariables.put(\"".concat(outputs.get(0).getName()).concat("\", ")
						.concat("result);"));
			}
		}
		addLine("\t}");
	}

	@Override
	public void addDecisionTableMethodHeader(final Decision decision) {
		addEmptyLine();
		addLine("\tprivate void decision"
				.concat(DmnUtils.namingConvention(decision.getId(), "intern"))
				.concat("(Map<String, Object> inputVariables) {"));
	}

	@Override
	public void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String source) {
		addLine("\t\tfinal ".concat(DmnUtils.getJavaVariableType(typeRef)).concat(" ")
				.concat(DmnUtils.namingConvention(variableName, "variable")).concat(" = ")
				.concat(source).concat(";"));
	}

	@Override
	public void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String requiredDecisionId, final Decision decision) {
		final String source = "decision"
				.concat(DmnUtils.namingConvention(requiredDecisionId, "intern")).concat("(")
				.concat(this.dmn.getInputVariablesString(false, decision)).concat(")");
		addDecisionTableMethodInputVariable(typeRef, variableName, source);
	}

	@Override
	public void addDecisionTableMethodRuleCondition(final Decision decision, final Rule rule,
			final List<Input> inputs, final boolean firstRule) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		final HitPolicy hitPolicy = table.getHitPolicy();
		final List<InputEntry> inputEntries = new ArrayList<>(rule.getInputEntries());
		final StringBuilder line = new StringBuilder();
		final String tableType = DmnUtils.getDecisionTableType(table);
		if (firstRule) {
			line.append("\t\tif (");
		} else {
			if (tableType.equals("11")) {
				line.append(hitPolicy == HitPolicy.COLLECT ? "\t\t}\n\t\tif (" : "\t\t} else if (");
			} else if (tableType.endsWith("n")) {
				line.append("\t\t}\n\t\tif (");
			} else {
				line.append("\t\t} else if (");
			}
		}
		int entry = 0;
		final StringJoiner joiner = new StringJoiner(" && ");
		for (final Input input : inputs) {
			final String inputEntry = inputEntries.get(entry++).getTextContent();
			joiner.add(buildCondition(decision, input, inputEntry.trim()));
		}
		line.append(joiner).append(") {");
		addStringBuilder(line);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addDecisionTableMethodRuleOutput(final Decision decision, final Rule rule) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		final String outputVariableName = this.dmn.getOutputVariableName(decision);
		final List<OutputEntry> outputEntries = new ArrayList<>(rule.getOutputEntries());
		final HitPolicy hitPolicy = table.getHitPolicy();
		final String outputClassName = "Output"
				.concat(DmnUtils.namingConvention(decision.getId(), "intern"));
		final List<Output> outputs = new ArrayList(table.getOutputs());
		final String mapName = DmnUtils.namingConvention(decision.getId(), "variable");
		switch (DmnUtils.getDecisionTableType(table)) {
		case "11":
			if (hitPolicy == HitPolicy.COLLECT) {
				final String outputText = outputEntries.get(0).getTextContent();
				switch (table.getAggregation()) {
				case SUM:
					addLine("\t\t\tresult += ".concat(outputText).concat(";"));
					break;
				case MIN:
					addLine("\t\t\tresult = Math.min(".concat(outputText).concat(", result);"));
					break;
				case MAX:
					addLine("\t\t\tresult = Math.max(".concat(outputText).concat(", result);"));
					break;
				case COUNT:
					addLine("\t\t\tresult++;");
					break;
				}
			} else {
				addLine("\t\t\tinputVariables.put(\"".concat(outputVariableName).concat("\", ")
						.concat(outputEntries.get(0).getTextContent()).concat(");"));
			}
			break;
		case "1n":
			addLine("\t\t\t".concat(mapName).concat(".put(\"").concat(outputVariableName)
					.concat("\", ").concat(outputEntries.get(0).getTextContent()).concat(");"));
			break;
		case "n1":

			for (int entryNr = 0; entryNr < rule.getOutputEntries().size(); entryNr++) {
				final OutputEntry entry = outputEntries.get(entryNr);
				final String outputName = outputs.get(entryNr).getName();
				addLine("\t\t\t".concat(mapName).concat(".put(\"").concat(outputName).concat("\", ")
						.concat(entry.getTextContent()).concat(");"));
			}
			break;
		case "nn":
			addLine("\t\t\t".concat(mapName).concat(" = new ").concat(outputClassName).concat("(")
					.concat(this.dmn.getOutputValuesString(rule)).concat(");"));
			addLine("\t\t\t" + mapName + "List.add(" + mapName + ");");
		}
	}

	@Override
	public void addDecisionTableMethodSubDecisionCall(final Decision requiredDecision) {
		addLine("\t\tdecision".concat(DmnUtils.namingConvention(requiredDecision.getId(), "intern"))
				.concat("(inputVariables);"));
	}

	@Override
	public void addDecisionTableMethodVariableMaps(final Decision decision) {
		if (DmnUtils.getDecisionTableType((DecisionTable) decision.getExpression()).equals("nn")) {
			addLine("\t\tfinal ArrayList<Output"
					.concat(DmnUtils.namingConvention(decision.getId(), "intern")).concat("> ")
					.concat(DmnUtils.namingConvention(decision.getId(), "variable"))
					.concat("List = new ArrayList<>();"));
			addLine("\t\tOutput".concat(DmnUtils.namingConvention(decision.getId(), "intern"))
					.concat(" ").concat(DmnUtils.namingConvention(decision.getId(), "variable"))
					.concat(" = null;"));
		} else {
			addLine("\t\tMap<String, Object> "
					.concat(DmnUtils.namingConvention(decision.getId(), "variable"))
					.concat(" = new HashMap<>();"));
		}
	}

	@Override
	public void addEmptyLine() {
		addLine("");
	}

	@Override
	public void addExecuteMethods() {
		switch (this.dmnEngine) {
		case standAlone:
			addLine("\tpublic ".concat(this.dmn.getTypeRefToReturn()).concat(" execute(")
					.concat(this.dmn.getInputVariablesString(true)).concat(") {"));
			final List<Input> inputs = this.dmn.getInputs(true, false, true,
					this.dmn.getMainDecision());
			addLine("\t\tfinal Map<String, Object> inputVariables = new HashMap<>();");
			for (final Input input : inputs) {
				if (!DmnUtils.getInputVariableType(input).endsWith("formula")) {
					final String inputVariableName = this.dmn.getInputVariableName(input,
							"container");
					addLine("\t\tinputVariables.put(\"".concat(inputVariableName)
							.concat("\", ".concat(inputVariableName).concat(");")));
				}
			}
			addLine("\t\treturn execute(inputVariables);");
			addLine("\t}");
			addEmptyLine();
			addLine("\tpublic ".concat(this.dmn.getTypeRefToReturn())
					.concat(" execute(final Map<String, Object> inputVariables) {"));
			addLine("\t\tdecision"
					.concat(DmnUtils.namingConvention(this.dmn.getMainDecision().getId(), "intern"))
					.concat("(inputVariables);"));
			final boolean tableNN = DmnUtils
					.getDecisionTableType(
							(DecisionTable) this.dmn.getMainDecision().getExpression())
					.equals("nn");
			addLine("\t\treturn "
					.concat(tableNN ? "new Gson().toJson("
							: "(".concat(this.dmn.getTypeRefToReturn()).concat(") "))
					.concat("inputVariables.get(\"")
					.concat(this.dmn.getOutputVariableName(this.dmn.getMainDecision()))
					.concat("\")".concat(tableNN ? ")" : "").concat(";")));
			break;
		case camunda:
			addLine("\t@Override");
			addLine("\tpublic void execute(final DelegateExecution execution) throws Exception {");
			addLine("\t\tMap<String, Object> inputVariables = execution.getVariables();");
			final String mainDecision = "decision".concat(
					DmnUtils.namingConvention(this.dmn.getMainDecision().getId(), "intern"));
			addLine("\t\t".concat(mainDecision).concat("(inputVariables);"));
			addCamundaExecuteMethodSetVariable();
		}
		addLine("\t}");
	}

	@Override
	public void addJavaFileImports(final String outputPackage) {
		addLine("package ".concat(outputPackage).concat(";"));
		addEmptyLine();
		if (this.dmnEngine == DmnEngine.camunda) {
			addLine("import org.camunda.bpm.engine.delegate.DelegateExecution;");
			addLine("import org.camunda.bpm.engine.delegate.JavaDelegate;");
			addEmptyLine();
		}
		if (this.dmn.usesDate()) {
			addLine("import java.time.LocalDateTime;");
		}
		if (this.dmn.usesCollection()) {
			addLine("import java.util.ArrayList;");
		}
		// if (this.dmn.usesFormula()) {
		addLine("import java.math.BigDecimal;");
		// }
		addLine("import java.util.HashMap;");
		addLine("import java.util.Map;");
		addEmptyLine();
		addLine("import org.kie.dmn.feel.lang.impl.FEELImpl;");
		addLine("// <!-- https://mvnrepository.com/artifact/org.kie/kie-dmn-feel -->");
		addEmptyLine();
		if (this.dmn.existsTableType("nn")) {
			addLine("import com.google.gson.Gson;");
			addLine("// <!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->");
			addEmptyLine();
		}
	}

	@Override
	public void addLine(final String line) {
		this.code.add(line);
	}

	@Override
	public void addMainClassHeader() {
		addLine("public class Dmn"
				.concat(DmnUtils.namingConvention(this.dmn.getDefinitions().getId(), "intern"))
				.concat(this.dmnEngine == DmnEngine.camunda ? "Delegate implements JavaDelegate"
						: "")
				.concat(" {"));
		addEmptyLine();
	}

	@Override
	public void addOutputClass(final DecisionTable decisionTable) {
		String decisionTableParentId = "";
		if (DmnUtils.isChildOf(decisionTable, DecisionImpl.class)) {
			decisionTableParentId = ((Decision) decisionTable.getParentElement()).getId();
		}
		addEmptyLine();
		addLine("class Output".concat(DmnUtils.namingConvention(decisionTableParentId, "intern"))
				.concat(" {"));
		for (final Output output : decisionTable.getOutputs()) {
			addLine("\t".concat(DmnUtils.getJavaVariableType(output.getTypeRef())).concat(" ")
					.concat(DmnUtils.namingConvention(output.getName(), "variable") + ";"));
		}
		addEmptyLine();
		addLine("\tpublic Output".concat(DmnUtils.namingConvention(decisionTableParentId, "intern"))
				.concat("(").concat(this.dmn.getOutputVariablesString(true, decisionTable))
				.concat(") {"));
		for (final Output output : decisionTable.getOutputs()) {
			addLine("\t\tthis.".concat(DmnUtils.namingConvention(output.getName(), "variable"))
					.concat(" = ")
					.concat(DmnUtils.namingConvention(output.getName(), "variable") + ";"));
		}
		addLine("\t}");
		addLine("}");
	}

	@Override
	public void addStringBuilder(final StringBuilder sb) {
		this.code.add(sb.toString());
	}

	@Override
	public String buildCondition(final Decision decision, final Input input, String match) {
		if (!match.equals("") && !match.equals("-")) {
			final StringBuilder condition = new StringBuilder();
			boolean negation = false;
			if (match.startsWith("not(") && match.endsWith(")")) {
				negation = true;
				match = match.substring(4, match.length() - 1);
				condition.append("!");
			}
			final ArrayList<String> parts = DmnUtils.splitMatch(match);
			condition.append((parts.size() > 1) || negation ? "(" : "");
			final StringJoiner joiner = new StringJoiner(" || ");
			for (final String part : parts) {
				joiner.add(buildConditionPart(decision, input, part.trim()));
			}
			condition.append(joiner);
			condition.append((parts.size() > 1) || negation ? ")" : "");
			return condition.toString();
		} else {
			return "true";
		}
	}

	@Override
	public String buildConditionPart(final Decision decision, final Input input,
			final String elem) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		final String inputVariableName = DmnUtils
				.namingConvention(this.dmn.getInputVariableName(input, "component"), "variable");
		final String typeRef = input.getInputExpression().getTypeRef()
				.substring(input.getInputExpression().getTypeRef().indexOf(":") == -1 ? 0
						: input.getInputExpression().getTypeRef().indexOf(":") + 1);
		switch (typeRef) {
		case "boolean":
			if (elem.equals("true")) {
				return inputVariableName;
			}
			if (elem.equals("false")) {
				return "!".concat(inputVariableName);
			}
			return "";
		case "date":
			if (DmnUtils.isRange(elem)) {
				final String range = elem.replaceAll("\\.\\.", "#")
						.replaceAll("date and time\\(", "").replaceAll("[\\[\\]\\(\\)]", "");
				final String[] limits = range.split("#");
				if (limits.length == 2) {
					final String from = limits[0];
					final String to = limits[1];
					return inputVariableName.concat(".isAfter(LocalDateTime.parse(").concat(from)
							.concat(")) && ").concat(inputVariableName)
							.concat(".isBefore(LocalDateTime.parse(").concat(to).concat("))");
				}
				return "";
			} else {
				final LocalDateTime date = LocalDateTime
						.parse(elem.substring(15, elem.length() - 2));
				return inputVariableName
						.concat(elem.startsWith("&lt;") || elem.startsWith("&gt;") ? " " : " == ")
						.concat("LocalDateTime.parse(\"")
						.concat(date.toString().replaceAll("&gt;", ">").replaceAll("&lt;", "<"))
						.concat("\")");
			}
		case "number":
		case "double":
		case "long":
		case "integer":
			if (DmnUtils.isRange(elem)) {
				final String[] limits = elem.split("\\.\\.");
				if (limits.length == 2) {
					final String from = limits[0];
					final String to = limits[1];
					return "(".concat(inputVariableName).concat(" >")
							.concat(limits[0].charAt(0) == '[' ? "= " : " ")
							.concat(from.substring(1, from.length())).concat(" && ")
							.concat(inputVariableName).concat(" <")
							.concat(limits[1].charAt(limits[1].length() - 1) == ']' ? "= " : " ")
							.concat(to.substring(0, to.length() - 1)).concat(")");
				}
				return "";
			} else {
				return inputVariableName
						.concat(elem.startsWith("<") || elem.startsWith(">") ? "" : " == ")
						.concat(elem);
			}
		case "string":
			if (!elem.isEmpty() && elem.startsWith("\"") && elem.endsWith("\"")) {
				return inputVariableName.concat(".equals(").concat(elem).concat(")");
			} else {
				for (final Input item : table.getInputs()) {
					if (this.dmn.getInputVariableName(item, "all").equals(elem)) {
						return inputVariableName.concat(".equals(").concat(elem).concat(")");
					} else {
						return "false";
					}
				}
			}
		default:
			return "false";
		}
	}

	@Override
	public List<String> getJavaFile() {
		return this.code;
	}

}
