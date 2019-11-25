package de.materna.dmntools.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionRule;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.DrgElement;
import org.camunda.bpm.model.dmn.instance.Expression;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.LiteralExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Variable;

import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnUtils;

public class DmnMetaModelImpl implements DmnMetaModel {
	public Definitions definitions;
	public DmnModelInstance dmnModelInstance;

	public DmnMetaModelImpl(final File source) {
		try {
			this.dmnModelInstance = Dmn.readModelFromFile(source);
		} catch (final Exception e) {
			System.err.println(e);
			System.exit(-1);
		}
		this.definitions = this.dmnModelInstance.getDefinitions();
	}

	@Override
	public String addInputNameToInputVariablesString(final boolean withVarType, final Input input,
			final String inputVariablesString) {
		final StringJoiner joiner = new StringJoiner(", ");
		if (inputVariablesString.length() > 0) {
			joiner.add(inputVariablesString);
		}
		final InputExpression expression = input.getInputExpression();
		final String typeRef = getExpressionVariableTypeRef(expression);
		final String varType = withVarType ? "final ".concat(typeRef).concat(" ") : "";
		if (DmnUtils.getInputVariableType(input).endsWith("formula")) {
			final List<String> inputNames = getFormulaInputNames(expression.getTextContent());
			for (final String inputName : inputNames) {
				final String name = DmnUtils.namingConvention(inputName, "variable");
				if (!joiner.toString().contains(name)
						&& !matchesOutput(inputName, getFormulaOutputs(input))) {
					joiner.add(varType.concat(name));
				}
			}
		} else {
			final String name = DmnUtils.namingConvention(getInputVariableName(input, "component"),
					"variable");
			joiner.add(varType.concat(name));
		}
		return joiner.toString();
	}

	@Override
	public void checkInputOutputMatches(final Decision decision, final List<Input> list,
			final boolean withMatches, final boolean withNonMatches) {
		if (decision != null) {
			final List<InformationRequirement> informationRequirements = new ArrayList<>(
					decision.getInformationRequirements());
			for (final InformationRequirement informationRequirement : informationRequirements) {
				final Decision requiredDecision = informationRequirement.getRequiredDecision();
				checkInputOutputMatches(requiredDecision, list, withMatches, withNonMatches);
			}
			removeInputOutputMatches(list, informationRequirements, withMatches, withNonMatches);
		}
	}

	@Override
	public boolean existsTableType(final String tableType) {
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					final DecisionTable table = (DecisionTable) decision.getExpression();
					if (DmnUtils.getDecisionTableType(table).equals(tableType)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public List<DecisionTable> getAllDecisionTables() {
		final List<DecisionTable> list = new ArrayList<>();
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					list.add((DecisionTable) decision.getExpression());
				}
			}
		}
		return list;
	}

	@Override
	public List<Input> getAllInputs() {
		final List<Input> inputs = new ArrayList<>();
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					inputs.addAll(((DecisionTable) decision.getExpression()).getInputs());
				}
			}
		}
		return inputs;
	}

	@Override
	public List<Output> getAllOutputs() {
		final List<Output> outputs = new ArrayList<>();
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					outputs.addAll(((DecisionTable) decision.getExpression()).getOutputs());
				}
			}
		}
		return outputs;
	}

	@Override
	public Definitions getDefinitions() {
		return this.definitions;
	}

	@Override
	public DmnModelInstance getDmnModelInstance() {
		return this.dmnModelInstance;
	}

	@Override
	public String getExpressionVariableTypeRef(final Expression expression) {
		final String expressionTypeRef = DmnUtils.getJavaVariableType(expression.getTypeRef());
		if (expressionTypeRef.equals("boolean")) {
			final Pattern stringPattern = Pattern.compile("[\".*\"]");
			if (stringPattern.matcher(expression.getTextContent()).matches()) {
				return "String";
			}
			final Pattern numberPattern = Pattern.compile("[0-9]+");
			if (numberPattern.matcher(expression.getTextContent()).matches()) {
				return "Number";
			}
		}
		return DmnUtils.getJavaVariableType(expressionTypeRef);
	}

	@Override
	public List<String> getFilteredPossibleInputNames(final String expressionText) {
		final List<String> possibleInputNames = new ArrayList<>();
		final String[] parts = expressionText.replaceAll("[()/+*-]", " ").trim().split(" ");
		for (final String part : parts) {
			if (!part.equals("")) {
				possibleInputNames.add(part);
			}
		}
		return possibleInputNames;
	}

	@Override
	public List<String> getFormulaInputNames(final String expressionText) {
		final List<String> possibleInputNames = new ArrayList<>();
		final Matcher matcher = DmnUtils.QUALIFIED_NAME().matcher(expressionText);
		while (matcher.find()) {
			possibleInputNames.add(matcher.group(0));
		}
		return possibleInputNames;
	}

	@Override
	public List<Output> getFormulaOutputs(final Input input) {
		final Decision decision = (Decision) input.getParentElement().getParentElement();
		final List<Output> formulaOutputs = new ArrayList<>();
		final String expressionText = input.getInputExpression().getTextContent();
		final List<String> inputNames = getFormulaInputNames(expressionText);
		for (final InformationRequirement requirement : decision.getInformationRequirements()) {
			if ((requirement.getRequiredDecision() != null) && DmnUtils.isDmnElementType(
					requirement.getRequiredDecision().getExpression(), DecisionTableImpl.class)) {
				final DecisionTable requiredTable = (DecisionTable) requirement
						.getRequiredDecision().getExpression();
				for (final Output output : requiredTable.getOutputs()) {
					for (final String inputName : inputNames) {
						final String component = inputName.substring(inputName.indexOf(".") + 1);
						if (output.getName().equals(component)) {
							formulaOutputs.add(output);
						}
					}
				}
			}
		}
		return formulaOutputs;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Input> getInputs(final boolean deep, final boolean withMatches,
			final boolean withNonMatches, final Decision decision) {
		final List<Input> list = new ArrayList<>();
		if (decision == null) {
			if (DmnUtils.isDmnElementType(getMainDecision().getExpression(),
					DecisionTableImpl.class)) {
				return getInputs(deep, withMatches, withNonMatches, getMainDecision());
			} else {
				return list;
			}
		}
		final DecisionTable table = (DecisionTable) decision.getExpression();
		if (deep) {
			list.addAll(getRequirementInputs(new ArrayList(decision.getInformationRequirements()),
					withMatches, withNonMatches));
		}
		for (final Input input : table.getInputs()) {
			list.add(input);
		}
		checkInputOutputMatches(decision, list, withMatches, withNonMatches);
		return list;
	}

	@Override
	public String getInputVariableName(final Input input, final String part) {
		if (input != null) {
			String variableName = null;
			final String inputExpressionText = input.getInputExpression().getTextContent();
			if (DmnUtils.isFeelName(inputExpressionText)) {
				variableName = inputExpressionText;
			} else if ((input.getCamundaInputVariable() != null)
					&& DmnUtils.isFeelName(input.getCamundaInputVariable())) {
				variableName = input.getCamundaInputVariable();
			}
			if (variableName != null) {
				final int dot = variableName.indexOf(".");
				if (dot != -1) {
					if (part.equals("variable")) {
						return variableName.substring(0, dot);
					} else if (part.equals("component")) {
						return variableName.substring(dot + 1);
					}
				}
				return variableName;
			}
			return "input" + DmnUtils.namingConvention(input.getId(), "intern");
		}
		return null;
	}

	@Override
	public String getInputVariablesString(final boolean withVarType) {
		return getInputVariablesString(withVarType, null);
	}

	@Override
	public String getInputVariablesString(final boolean withVarType, final Decision decision) {
		String inputVariablesString = "";
		final List<Input> inputs = getInputs(true, false, true, decision);
		for (final Input input : inputs) {
			final String inputName = DmnUtils
					.namingConvention(getInputVariableName(input, "variable"), "variable");
			if ((inputName != null) && !inputVariablesString.contains(inputName)) {
				inputVariablesString = addInputNameToInputVariablesString(withVarType, input,
						inputVariablesString);
			}
		}
		return inputVariablesString;
	}

	@Override
	public Decision getMainDecision() {
		final ArrayList<Decision> requiredDecisions = new ArrayList<>();
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final ArrayList<InformationRequirement> informationRequirements = new ArrayList<>(
						((Decision) drgElement).getInformationRequirements());
				for (final InformationRequirement requirement : informationRequirements) {
					if (requirement.getRequiredDecision() != null) {
						requiredDecisions.add(requirement.getRequiredDecision());
					}
				}
			}
		}
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)
					&& !requiredDecisions.contains(drgElement)) {
				return (Decision) drgElement;
			}
		}
		return null;
	}

	@Override
	public String getOutputValuesString(final DecisionRule rule) {
		String outputValuesString = "";
		if ((rule.getOutputEntries() != null) && !rule.getOutputEntries().isEmpty()) {
			for (final LiteralExpression output : rule.getOutputEntries()) {
				outputValuesString += output.getTextContent().concat(", ");
			}
			if (outputValuesString.length() > 2) {
				return outputValuesString.substring(0, outputValuesString.length() - 2);
			}
			return outputValuesString;
		}
		return null;
	}

	@Override
	public Variable getOutputVariable(final DecisionTable decisionTable) {
		if (DmnUtils.isChildOf(decisionTable, DecisionImpl.class)
				&& (((Decision) decisionTable.getParentElement()).getVariable() != null)) {
			return ((Decision) decisionTable.getParentElement()).getVariable();
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public String getOutputVariableName(final Decision decision) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		final Variable variable = getOutputVariable(table);
		if (variable == null) {
			switch (DmnUtils.getDecisionTableType(table)) {
			case "11":
			case "1n":
				return DmnUtils.namingConvention(
						((Output) new ArrayList(table.getOutputs()).get(0)).getName(), "variable");
			case "n1":
				return DmnUtils.namingConvention(decision.getId(), "variable");
			case "nn":
				return DmnUtils.namingConvention(decision.getId(), "variable").concat("List");
			}
		}
		return DmnUtils.namingConvention(variable.getName(), "variable");
	}

	@Override
	public String getOutputVariablesString(final boolean withVarType,
			final DecisionTable currentDecisionTable) {
		final StringBuilder outputVariablesString = new StringBuilder();
		final List<Output> outputs = new ArrayList<>(currentDecisionTable.getOutputs());
		for (final Output output : outputs) {
			final String typeRef = DmnUtils.getJavaVariableType(output.getTypeRef());
			outputVariablesString.append(withVarType ? "final ".concat(typeRef).concat(" ") : "")
					.append(DmnUtils.namingConvention(output.getName(), "variable")).append(", ");
		}
		if (outputVariablesString.length() > 2) {
			return outputVariablesString.toString().substring(0,
					outputVariablesString.length() - 2);
		}
		return outputVariablesString.toString();
	}

	@Override
	public List<Decision> getRequirementDecisions(final List<InformationRequirement> requirements) {
		final List<Decision> decisions = new ArrayList<>();
		for (final InformationRequirement requirement : requirements) {
			final Decision decision = requirement.getRequiredDecision();
			if ((decision != null) && DmnUtils.isDmnElementType(decision.getExpression(),
					DecisionTableImpl.class)) {
				decisions.add(decision);
			}
		}
		return decisions;
	}

	@Override
	public List<Input> getRequirementInputs(final List<InformationRequirement> requirementsList,
			final boolean withMatches, final boolean withNonMatches) {
		final List<Input> list = new ArrayList<>();
		for (final Decision decision : getRequirementDecisions(requirementsList)) {
			for (final Input input : getInputs(true, withMatches, withNonMatches, decision)) {
				if (!list.contains(input)) {
					list.add(input);
				}
			}
		}
		return list;
	}

	@Override
	public String getTypeRefToReturn() {
		final DecisionTable table = (DecisionTable) getMainDecision().getExpression();
		if (table != null) {
			if ((table.getHitPolicy() == HitPolicy.COLLECT) && (table.getAggregation() != null)) {
				return "Number";
			} else {
				if (DmnUtils.getDecisionTableType(table).equals("11")) {
					final List<Output> outputs = new ArrayList<>(table.getOutputs());
					return DmnUtils.getJavaVariableType(outputs.get(0).getTypeRef());
				} else {
					return "String";
				}
			}
		}
		return null;
	}

	@Override
	public boolean matchesOutput(final String inputName, final List<Output> outputs) {
		for (final Output output : outputs) {
			if (output.getName().equals(inputName.substring(inputName.indexOf(".") + 1))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void removeInputOutputMatches(final List<Input> list,
			final List<InformationRequirement> requirementsList, final boolean withMatches,
			final boolean withNonMatches) {
		final List<DmnElement> toRemove = new ArrayList<>();
		if (!withNonMatches) {
			toRemove.addAll(list);
		}
		for (final Decision decision : getRequirementDecisions(requirementsList)) {
			for (final Output output : ((DecisionTable) decision.getExpression()).getOutputs()) {
				for (final Input input : list) {
					if (getInputVariableName(input, "component").equals(output.getName())) {
						if (withMatches) {
							toRemove.remove(input);
						} else {
							toRemove.add(input);
						}
					}
				}
			}
		}
		list.removeAll(toRemove);
	}

	@Override
	public void setDefinitions(final Definitions definitions) {
		this.definitions = definitions;
	}

	@Override
	public void setDmnModelInstance(final DmnModelInstance dmnModelInstance) {
		this.dmnModelInstance = dmnModelInstance;
	}

	@Override
	public boolean usesCollection() {
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			Expression expression = null;
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				expression = ((Decision) drgElement).getExpression();
				if (DmnUtils.isDmnElementType(expression, DecisionTableImpl.class)) {
					if (DmnUtils.getDecisionTableType((DecisionTable) expression).endsWith("n")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean usesDate() {
		for (final Input input : getAllInputs()) {
			final String typeRef = input.getInputExpression().getTypeRef();
			if ((typeRef != null) && typeRef.contains("date")) {
				return true;
			}
		}
		for (final Output output : getAllOutputs()) {
			final String typeRef = output.getTypeRef();
			if ((typeRef != null) && typeRef.contains("date")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean usesFormula() {
		for (final Input input : getAllInputs()) {
			if (DmnUtils.getInputVariableType(input).endsWith("formula")) {
				return true;
			}
		}
		return false;
	}
}