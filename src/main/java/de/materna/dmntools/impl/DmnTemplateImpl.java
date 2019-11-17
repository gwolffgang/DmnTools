package de.materna.dmntools.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.impl.instance.InputImpl;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputEntry;
import org.camunda.bpm.model.dmn.instance.LiteralExpression;
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
	public void addCamundaExecuteMethodGetVariables() {
		final String inputsString = this.dmn.getInputVariablesString(true).replaceAll("final ", "");
		if (!inputsString.isEmpty()) {
			final List<String> variables = Arrays.asList(inputsString.split(", "));
			for (final String variable : variables) {
				final List<String> parts = Arrays.asList(variable.split(" "));
				addLine("\t\tfinal ".concat(DmnUtils.getJavaVariableType(parts.get(0))).concat(" ")
						.concat(parts.get(1)).concat(" = (").concat(parts.get(0))
						.concat(") execution.getVariable(\"").concat(parts.get(1)).concat("\");"));
			}
		}
	}

	@Override
	public void addCamundaExecuteMethodMainMethodCall() {
		addLine("\t\tfinal "
				.concat(this.dmn.getTypeRefToReturn(
						(DecisionTable) this.dmn.getMainDecision().getExpression(), this.dmnEngine))
				.concat(" resultOfDmn = decision")
				.concat(DmnUtils.namingConvention(this.dmn.getMainDecision().getId(), "intern"))
				.concat("(").concat(this.dmn.getInputVariablesString(false) + ");"));
	}

	@Override
	public void addCamundaExecuteMethodSetVariable() {
		if ((this.dmn.getMainDecision() != null) && DmnUtils.isDmnElementType(
				this.dmn.getMainDecision().getExpression(), DecisionTableImpl.class)) {
			final List<Output> outputs = new ArrayList<>(
					((DecisionTable) this.dmn.getMainDecision().getExpression()).getOutputs());
			final String resultName = "result"
					.concat(DmnUtils.namingConvention(this.dmn.getDefinitions().getId(), "intern"));
			addLine("\t\texecution.setVariable(\""
					.concat((outputs.size() == 1) && (outputs.get(0).getName() != null)
							? outputs.get(0).getName()
							: resultName)
					.concat("\", resultOfDmn);"));
		}
	}

	@Override
	public void addDecisionTableMethodFooter() {
		addLine("\t\t}");
		final String outputVariableName = this.dmn
				.getOutputVariableName(this.dmn.getCurrentDecisionTable());
		if (DmnUtils.getDecisionTableType(this.dmn.getCurrentDecisionTable()).equals("nn")) {
			addLine("\t\t".concat(outputVariableName).concat(" = new Gson().toJson(outputList);"));
		}
		addLine("\t\treturn ".concat(outputVariableName).concat(";"));
		addLine("\t}");
	}

	@Override
	public void addDecisionTableMethodHeader(final String methodName) {
		addEmptyLine();
		addLine("\tprivate "
				.concat(this.dmn.getTypeRefToReturn(this.dmn.getCurrentDecisionTable(),
						this.dmnEngine))
				.concat(" decision").concat(DmnUtils.namingConvention(methodName, "intern"))
				.concat("(")
				.concat(this.dmn.getInputVariablesString(true, this.dmn.getCurrentDecisionTable()))
				.concat(") {"));
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
			final String requiredDecisionId, final DecisionTable decisionTable) {
		final String source = "decision"
				.concat(DmnUtils.namingConvention(requiredDecisionId, "intern")).concat("(")
				.concat(this.dmn.getInputVariablesString(false, decisionTable)).concat(")");
		addDecisionTableMethodInputVariable(typeRef, variableName, source);
	}

	@Override
	public void addDecisionTableMethodOutputVariable(final String variableType,
			final String variableName, final String initiation) {
		addLine("\t\t".concat(variableType).concat(" ").concat(variableName).concat(" = ")
				.concat(initiation).concat(";"));
		addEmptyLine();
	}

	@Override
	public void addDecisionTableMethodRuleCondition(final Rule rule, final List<DmnElement> elems,
			final boolean firstRule) {
		final HitPolicy hitPolicy = this.dmn.getCurrentDecisionTable().getHitPolicy();
		final List<InputEntry> inputEntries = new ArrayList<>(rule.getInputEntries());
		final StringBuilder line = new StringBuilder();
		final String tableType = DmnUtils.getDecisionTableType(this.dmn.getCurrentDecisionTable());
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
		for (final DmnElement elem : elems) {
			if (DmnUtils.isDmnElementType(elem, InputImpl.class)) {
				final String inputEntry = inputEntries.get(entry++).getTextContent();
				joiner.add(buildCondition((Input) elem, inputEntry.trim()));
			}
		}
		line.append(joiner).append(") {");
		addStringBuilder(line);
	}

	@Override
	public void addDecisionTableMethodRuleOutput(final Rule rule) {
		final String outputVariableName = this.dmn
				.getOutputVariableName(this.dmn.getCurrentDecisionTable());
		final List<Output> outputs = new ArrayList<>(
				this.dmn.getCurrentDecisionTable().getOutputs());
		final List<OutputEntry> outputEntries = new ArrayList<>(rule.getOutputEntries());
		final HitPolicy hitPolicy = this.dmn.getCurrentDecisionTable().getHitPolicy();
		switch (DmnUtils.getDecisionTableType(this.dmn.getCurrentDecisionTable())) {
		case "11":
			if (hitPolicy == HitPolicy.COLLECT) {
				final String outputText = outputEntries.get(0).getTextContent();
				switch (this.dmn.getCurrentDecisionTable().getAggregation()) {
				case SUM:
					addLine("\t\t\t".concat(outputVariableName).concat(" += ").concat(outputText)
							.concat(";"));
					break;
				case MIN:
					addLine("\t\t\t".concat(outputVariableName).concat(" = Math.min(")
							.concat(outputText).concat(", output);"));
					break;
				case MAX:
					addLine("\t\t\t".concat(outputVariableName).concat(" = Math.max(")
							.concat(outputText).concat(", output);"));
					break;
				case COUNT:
					addLine("\t\t\t".concat(outputVariableName).concat("++;"));
					break;
				}
			} else {
				boolean isEnum = false;
				final String typeRef = this.dmn
						.getTypeRefToReturn(this.dmn.getCurrentDecisionTable(), this.dmnEngine);
				if (!typeRef.equals("String") && !typeRef.equals("boolean")
						&& !typeRef.equals("double") && !typeRef.equals("int")
						&& !typeRef.equals("LocalDateTime")) {
					isEnum = true;
				}
				addLine("\t\t\t".concat(outputVariableName).concat(isEnum ? ".value = " : " = ")
						.concat(outputEntries.get(0).getTextContent()).concat(";"));
			}
			break;
		case "1n":
			addLine("\t\t\t".concat(outputVariableName).concat(".add(")
					.concat(outputEntries.get(0).getTextContent()).concat(");"));
			break;
		case "n1":
			for (int output = 0; output < rule.getOutputEntries().size(); output++) {
				final LiteralExpression outputEntry = outputEntries.get(output);
				addLine("\t\t\t".concat(outputVariableName).concat(".put(\"")
						.concat(outputs.get(output).getName()).concat("\", ")
						.concat(outputEntry.getTextContent()).concat(");"));
			}
			break;
		case "nn":
			final String outputMethodName = "Output".concat(
					DmnUtils.namingConvention(this.dmn.getCurrentDecision().getId(), "intern"));
			addLine("\t\t\t".concat(outputMethodName).concat(" result = new ")
					.concat(outputMethodName).concat("(")
					.concat(this.dmn.getOutputValuesString(rule)).concat(");"));
			addLine("\t\t\toutputList.add(result);");
		}
	}

	@Override
	public void addEmptyLine() {
		addLine("");
	}

	@Override
	public void addExecuteMethod() {
		switch (this.dmnEngine) {
		case standAlone:
			addLine("\tpublic "
					.concat(this.dmn.getTypeRefToReturn(
							(DecisionTable) this.dmn.getMainDecision().getExpression(),
							this.dmnEngine))
					.concat(" execute(").concat(this.dmn.getInputVariablesString(true))
					.concat(") {"));
			addLine("\t\treturn decision"
					.concat(DmnUtils.namingConvention(this.dmn.getMainDecision().getId(), "intern"))
					.concat("(").concat(this.dmn.getInputVariablesString(false)).concat(");"));
			break;
		case camunda:
			addLine("\t@Override");
			addLine("\tpublic void execute(final DelegateExecution execution) throws Exception {");
			addCamundaExecuteMethodGetVariables();
			addCamundaExecuteMethodMainMethodCall();
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
			if (this.dmn.existsTableType("n1")) {
				addLine("import org.camunda.bpm.engine.variable.VariableMap;");
				addLine("import org.camunda.bpm.engine.variable.Variables;");
			}
			addEmptyLine();
		}
		boolean emptyLineNeeded = false;
		if (this.dmn.usesDate()) {
			addLine("import java.time.LocalDateTime;");
			emptyLineNeeded = true;
		}
		if (this.dmn.usesCollection("ArrayList")) {
			addLine("import java.util.ArrayList;");
			emptyLineNeeded = true;
		}
		if (this.dmn.usesCollection("Map")) {
			addLine("import java.util.HashMap;");
			addLine("import java.util.Map;");
			emptyLineNeeded = true;
		}
		if (emptyLineNeeded) {
			addEmptyLine();
		}
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
		addLine("public class "
				.concat(DmnUtils.namingConvention(this.dmn.getDefinitions().getId(), "class"))
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
	public String buildCondition(final Input input, String match) {
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
				joiner.add(buildConditionPart(input, part.trim()));
			}
			condition.append(joiner);
			condition.append((parts.size() > 1) || negation ? ")" : "");
			return condition.toString();
		} else {
			return "true";
		}
	}

	@Override
	public String buildConditionPart(final Input input, final String elem) {
		final String inputVariableName = DmnUtils
				.namingConvention(this.dmn.getInputVariableName(input), "variable");
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
				for (final Input item : this.dmn.getCurrentDecisionTable().getInputs()) {
					if (this.dmn.getInputVariableName(item).equals(elem)) {
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
