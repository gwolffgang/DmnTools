package de.materna.dmntools.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.dmn.BuiltinAggregator;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.impl.instance.InputExpressionImpl;
import org.camunda.bpm.model.dmn.impl.instance.InputImpl;
import org.camunda.bpm.model.dmn.impl.instance.OutputImpl;
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

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnUtils;

public class DmnMetaModelImpl implements DmnMetaModel {
	public Decision currentDecision = null;
	public DecisionTable currentDecisionTable = null;
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
			final String currentString) {
		final StringJoiner joiner = new StringJoiner(", ");
		if (currentString.length() > 0) {
			joiner.add(currentString);
		}
		final InputExpression expression = input.getInputExpression();
		final String typeRef = DmnUtils.getJavaVariableType(expression.getTypeRef());
		final String varType = withVarType ? "final ".concat(typeRef).concat(" ") : "";
		if (getInputVariableType(input).endsWith("formula")) {
			final List<String> inputNames = getFormulaInputNames(expression.getTextContent());
			for (final String inputName : inputNames) {
				final String name = DmnUtils.namingConvention(inputName, "variable");
				if (!joiner.toString().contains(name)
						&& !matchesOutput(inputName, getFormulaOutputs(input))) {
					joiner.add(varType.concat(name));
				}
			}
		} else {
			final String name = DmnUtils.namingConvention(getInputVariableName(input), "variable");
			joiner.add(varType.concat(name));
		}
		return joiner.toString();
	}

	@Override
	public void checkInputOutputMatches(final DecisionTable decisionTable,
			final List<DmnElement> list) {
		final Decision decision = (Decision) decisionTable.getParentElement();
		final Collection<InformationRequirement> informationRequirements = decision
				.getInformationRequirements();
		for (final InformationRequirement informationRequirement : informationRequirements) {
			final Decision requiredDecision = informationRequirement.getRequiredDecision();
			if (requiredDecision != null) {
				checkInputOutputMatches((DecisionTable) requiredDecision.getExpression(), list);
			}
		}
		removeInputOutputMatches(list, informationRequirements);
	}

	@Override
	public List<List<String>> combineStrings(final List<String> list, final int elemsToCombine,
			final String original) {
		final List<List<String>> combinedStrings = new ArrayList<>();
		for (int k = 0; k < (list.size() - (elemsToCombine - 1)); k++) {
			final List<String> listItem = new ArrayList<>();
			String str = list.get(k);
			for (int elem = 1; elem < elemsToCombine; elem++) {
				str = str.concat(DmnUtils.namingConvention(list.get(k + elem), "intern"));
			}
			listItem.add(DmnUtils.namingConvention(str, "variable"));
			final String string = list.get(k);
			final String preString = k > 0 ? list.get(k - 1) : "";
			final String lastString = list.get(k + (elemsToCombine - 1));
			final String preLastString = k > 0 ? list.get((k + (elemsToCombine - 1)) - 1) : "";
			final int stringStart = original.indexOf(preString) + preString.length();
			final int lastStringStart = original.indexOf(preLastString) + preLastString.length();
			final String remainder = original
					.substring(original.indexOf(lastString, lastStringStart) + lastString.length());
			final int remainderStart = original.indexOf(remainder) > 0 ? original.indexOf(remainder)
					: original.length();
			listItem.add(original.substring(original.indexOf(string, stringStart), remainderStart));
			combinedStrings.add(listItem);
		}
		return combinedStrings;
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
				this.currentDecision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(this.currentDecision.getExpression(),
						DecisionTableImpl.class)) {
					list.add((DecisionTable) this.currentDecision.getExpression());
				}
			}
		}
		return list;
	}

	@Override
	public String getCorrectedFormula(final Input input) {
		final List<String> inputNames = getFormulaInputNames(
				input.getInputExpression().getTextContent());
		String correctedFormula = input.getInputExpression().getTextContent();
		for (final String inputName : inputNames) {
			correctedFormula = correctedFormula.substring(0, correctedFormula.indexOf(inputName))
					.concat(DmnUtils.namingConvention(inputName, "variable"))
					.concat(correctedFormula
							.substring(correctedFormula.indexOf(inputName) + inputName.length()));
		}
		return correctedFormula;
	}

	@Override
	public Decision getCurrentDecision() {
		return this.currentDecision;
	}

	@Override
	public DecisionTable getCurrentDecisionTable() {
		return this.currentDecisionTable;
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
		final Pattern pattern = Pattern.compile(DmnUtils.buildVariableNameRegEx());
		final Matcher matcher = pattern.matcher(expressionText);
		while (matcher.find()) {
			possibleInputNames.add(matcher.group(0));
		}
		return possibleInputNames;
	}

	@Override
	public List<Output> getFormulaOutputs(final Input input) {
		final List<Output> formulaOutputs = new ArrayList<>();
		final String expressionText = input.getInputExpression().getTextContent();
		final List<String> inputNames = getFormulaInputNames(expressionText);
		if (DmnUtils.isDmnElementType((DmnElement) input.getParentElement().getParentElement(),
				DecisionImpl.class)) {
			final Decision decision = (Decision) input.getParentElement().getParentElement();
			for (final InformationRequirement requirement : decision.getInformationRequirements()) {
				if ((requirement.getRequiredDecision() != null) && DmnUtils.isDmnElementType(
						requirement.getRequiredDecision().getExpression(),
						DecisionTableImpl.class)) {
					final DecisionTable requiredTable = (DecisionTable) requirement
							.getRequiredDecision().getExpression();
					for (final Output output : requiredTable.getOutputs()) {
						for (final String inputName : inputNames) {
							if (output.getName().equals(inputName)) {
								formulaOutputs.add(output);
							}
						}
					}
				}
			}
		}
		return formulaOutputs;
	}

	@Override
	public List<DmnElement> getInAndOutputs(final boolean matched, final boolean deep,
			final DecisionTable decisionTable) {
		final List<DmnElement> list = new ArrayList<>();
		if (decisionTable == null) {
			if (DmnUtils.isDmnElementType(getMainDecision().getExpression(),
					DecisionTableImpl.class)) {
				return getInAndOutputs(matched, deep,
						(DecisionTable) getMainDecision().getExpression());
			} else {
				return list;
			}
		}
		if (deep) {
			final Decision decision = (Decision) decisionTable.getParentElement();
			list.addAll(getRequirementInAndOutputs(decision.getInformationRequirements(), matched));
		}
		for (final Input input : decisionTable.getInputs()) {
			list.add(input);
			if (getInputVariableType(input).endsWith("formula") && deep) {
				list.addAll(getFormulaOutputs(input));
			}
		}
		if (matched) {
			checkInputOutputMatches(decisionTable, list);
		}
		removeOutputInputMatches(list);
		return list;
	}

	@Override
	public String getInputVariableName(final Input input) {
		if (input != null) {
			final Expression expression = input.getInputExpression();
			final String camundaVariableName = input.getCamundaInputVariable();
			if (camundaVariableName != null) {
				return camundaVariableName;
			} else {
				if (DmnUtils.isDmnElementType(expression, InputExpressionImpl.class)) {
					final String text = ((LiteralExpression) expression).getTextContent();
					final String variableName = text.substring(0,
							text.indexOf("\"") > -1 ? text.indexOf("\"") : text.length());
					if (DmnUtils.isVariableName(text)) {
						return variableName;
					}
				}
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
	public String getInputVariablesString(final boolean withVarType,
			final DecisionTable decisionTable) {
		String inputVariablesString = "";
		final List<DmnElement> inAndOutputs = getInAndOutputs(true, true, decisionTable);
		for (final DmnElement inOrOutput : inAndOutputs) {
			if (DmnUtils.isDmnElementType(inOrOutput, InputImpl.class)) {
				final Input input = (Input) inOrOutput;
				final String inputName = DmnUtils.namingConvention(getInputVariableName(input),
						"variable");
				if ((inputName != null) && !inputVariablesString.contains(inputName)) {
					inputVariablesString = addInputNameToInputVariablesString(withVarType, input,
							inputVariablesString);
				}
			}
		}
		return inputVariablesString;
	}

	@Override
	public String getInputVariableType(final Input input) {
		final String name = getInputVariableName(input);
		final String inputExpression = input.getInputExpression().getTextContent();
		if (name.equals(inputExpression)) {
			return "named empty";
		}
		final boolean isNamed = DmnUtils.isVariableName(name);
		final boolean isVariable = DmnUtils.isVariableName(inputExpression);
		final boolean isEmpty = inputExpression.equals("");
		return (isNamed ? "named " : "unnamed ")
				.concat(isVariable ? "variable" : (isEmpty ? "empty" : "formula"));
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

	@Override
	public String getOutputVariableName(final DecisionTable decisionTable) {
		final Variable variable = getOutputVariable(decisionTable);
		return variable != null ? DmnUtils.namingConvention(variable.getName(), "variable")
				: "output";
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
	public List<DecisionTable> getRequirementDecisionTables(
			final List<InformationRequirement> requirements) {
		final List<DecisionTable> decisionTables = new ArrayList<>();
		for (final InformationRequirement requirement : requirements) {
			final Decision decision = requirement.getRequiredDecision();
			if ((decision != null) && DmnUtils.isDmnElementType(decision.getExpression(),
					DecisionTableImpl.class)) {
				decisionTables.add((DecisionTable) decision.getExpression());
			}
		}
		return decisionTables;
	}

	@Override
	public List<DmnElement> getRequirementInAndOutputs(
			final Collection<InformationRequirement> requirementsList, final boolean matched) {
		final List<InformationRequirement> requirements = new ArrayList<>(requirementsList);
		final List<DmnElement> list = new ArrayList<>();
		for (final DecisionTable decisionTable : getRequirementDecisionTables(requirements)) {
			for (final DmnElement elem : getInAndOutputs(matched, true, decisionTable)) {
				if (!list.contains(elem)) {
					list.add(elem);
				}
			}
		}
		return list;
	}

	@Override
	public String getTypeRefToReturn(final DecisionTable decisionTable, final DmnEngine dmnEngine) {
		if (decisionTable != null) {
			final Variable variable = getOutputVariable(decisionTable);
			if ((variable != null) && !variable.getName().equals("")) {
				return DmnUtils.getJavaVariableType(variable.getTypeRef());
			}
			if ((decisionTable.getHitPolicy() == HitPolicy.COLLECT)
					&& (decisionTable.getAggregation() != null)
					&& (decisionTable.getAggregation() == BuiltinAggregator.COUNT)) {
				return "int";
			} else {
				final List<Output> outputs = new ArrayList<>(decisionTable.getOutputs());
				switch (DmnUtils.getDecisionTableType(decisionTable)) {
				case "11":
					return DmnUtils.getJavaVariableType(outputs.get(0).getTypeRef());
				case "1n":
					return "ArrayList<"
							.concat(DmnUtils.getJavaVariableClass(outputs.get(0).getTypeRef()))
							.concat(">");
				case "n1":
					return dmnEngine == DmnEngine.camunda ? "VariableMap" : "Map<String, Object>";
				case "nn":
					return "String";
				default:
					return null;
				}
			}
		}
		return null;
	}

	@Override
	public boolean matchesOutput(final String inputName, final List<Output> outputs) {
		for (final Output output : outputs) {
			if (output.getName().equals(inputName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void removeInputOutputMatches(final List<DmnElement> list,
			final Collection<InformationRequirement> requirementsList) {
		final List<InformationRequirement> requirements = new ArrayList<>(requirementsList);
		final List<DmnElement> toRemove = new ArrayList<>();
		for (final DecisionTable decisionTable : getRequirementDecisionTables(requirements)) {
			for (final Output output : decisionTable.getOutputs()) {
				for (final DmnElement listElem : list) {
					if (DmnUtils.isDmnElementType(listElem, InputImpl.class)) {
						final Input input = (Input) listElem;
						if (getInputVariableName(input).equals(output.getName())) {
							toRemove.add(listElem);
						}
					}
				}
			}
		}
		list.removeAll(toRemove);
	}

	@Override
	public void removeOutputInputMatches(final List<DmnElement> list) {
		final List<DmnElement> toRemove = new ArrayList<>();
		for (final DmnElement outputElem : list) {
			if (DmnUtils.isDmnElementType(outputElem, OutputImpl.class)) {
				final Output output = (Output) outputElem;
				for (final DmnElement inputElem : list) {
					if (DmnUtils.isDmnElementType(inputElem, InputImpl.class)) {
						final Input input = (Input) inputElem;
						if (output.getName().equals(getInputVariableName(input))) {
							toRemove.add(output);
						}
					}
				}
			}
		}
		list.removeAll(toRemove);
	}

	@Override
	public void setCurrentDecision(final Decision decision) {
		this.currentDecision = decision;
	}

	@Override
	public void setCurrentDecisionTable(final DecisionTable decisionTable) {
		this.currentDecisionTable = decisionTable;
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
	public boolean usesCollection(final String collectionType) {
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			Expression expression = null;
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				expression = ((Decision) drgElement).getExpression();
			}
			if (DmnUtils.isDmnElementType(expression, DecisionTableImpl.class)) {
				final String tableType = DmnUtils.getDecisionTableType((DecisionTable) expression);
				switch (collectionType) {
				case "ArrayList":
					if (tableType.endsWith("n")) {
						return true;
					}
					break;
				case "Map":
					if (tableType.equals("n1")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean usesDate() {
		for (final DrgElement drgElement : this.definitions.getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					final DecisionTable table = (DecisionTable) decision.getExpression();
					for (final Input input : table.getInputs()) {
						final String typeRef = input.getInputExpression().getTypeRef();
						if ((typeRef != null) && typeRef.contains("date")) {
							return true;
						}
					}
					for (final Output output : table.getOutputs()) {
						final String typeRef = output.getTypeRef();
						if ((typeRef != null) && typeRef.contains("date")) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}