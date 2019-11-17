package de.materna.dmntools.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.impl.instance.InputExpressionImpl;
import org.camunda.bpm.model.dmn.impl.instance.InputImpl;
import org.camunda.bpm.model.dmn.impl.instance.OutputImpl;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.DrgElement;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnStructure;
import de.materna.dmntools.DmnTemplate;
import de.materna.dmntools.DmnUtils;

public class DmnStructureImpl implements DmnStructure {
	public final DmnMetaModel dmn;

	public DmnEngine dmnEngine;

	public DmnStructureImpl(final DmnMetaModel dmn) {
		this.dmn = dmn;
	}

	@Override
	public void addDecisionTableMethod(final DmnTemplate code) {
		final String methodName = this.dmn.getCurrentDecision().getId();
		code.addDecisionTableMethodHeader(methodName);
		addDecisionTableMethodInputVariables(
				this.dmn.getInAndOutputs(false, true, this.dmn.getCurrentDecisionTable()), code);
		addDecisionTableMethodOutputVariable(code);
		addDecisionTableMethodRules(
				this.dmn.getInAndOutputs(false, false, this.dmn.getCurrentDecisionTable()), code);
		code.addDecisionTableMethodFooter();
	}

	@Override
	public void addDecisionTableMethodInputVariable(final DmnElement elem, final DmnTemplate code) {
		if (DmnUtils.isDmnElementType(elem, InputImpl.class)) {
			final Input input = (Input) elem;
			if (DmnUtils.isDmnElementType(input.getInputExpression(), InputExpressionImpl.class)) {
				final String expressionText = input.getInputExpression().getTextContent();
				switch (this.dmn.getInputVariableType(input)) {
				case "named variable":
				case "unnamed variable":
				case "named empty":
					for (final InformationRequirement informationRequirement : this.dmn
							.getCurrentDecision().getInformationRequirements()) {
						if (informationRequirement.getRequiredDecision() != null) {
							final Decision requiredDecision = informationRequirement
									.getRequiredDecision();
							if (DmnUtils.isDmnElementType(requiredDecision.getExpression(),
									DecisionTableImpl.class)) {
								final DecisionTable decisionTable = (DecisionTable) requiredDecision
										.getExpression();
								for (final Output output : decisionTable.getOutputs()) {
									if (output.getName()
											.equals(this.dmn.getInputVariableName(input))) {
										code.addDecisionTableMethodInputVariable(
												input.getInputExpression().getTypeRef(),
												output.getName(), requiredDecision.getId(),
												decisionTable);
									}
								}
							}
						}
					}
					if (this.dmn.getInputVariableType(input).equals("named variable")) {
						code.addDecisionTableMethodInputVariable(
								input.getInputExpression().getTypeRef(),
								this.dmn.getInputVariableName(input),
								DmnUtils.namingConvention(expressionText, "variable"));
					}
					break;
				case "named formula":
				case "unnamed formula":
					code.addDecisionTableMethodInputVariable(
							input.getInputExpression().getTypeRef(),
							this.dmn.getInputVariableName(input),
							this.dmn.getCorrectedFormula(input));
					break;
				}
			}
		} else { // Outputs
			final Output output = (Output) elem;
			code.addDecisionTableMethodInputVariable(output.getTypeRef(), output.getName(),
					((Decision) output.getParentElement().getParentElement()).getId(),
					(DecisionTable) output.getParentElement());
		}
	}

	@Override
	public void addDecisionTableMethodInputVariables(final List<DmnElement> elems,
			final DmnTemplate code) {
		for (final DmnElement elem : elems) {
			if (DmnUtils.isDmnElementType(elem, OutputImpl.class)) {
				addDecisionTableMethodInputVariable(elem, code);
			}
		}
		for (final DmnElement elem : elems) {
			if (DmnUtils.isDmnElementType(elem, InputImpl.class)
					&& !this.dmn.getInputVariableType((Input) elem).endsWith("formula")) {
				addDecisionTableMethodInputVariable(elem, code);
			}
		}
		for (final DmnElement elem : elems) {
			if (DmnUtils.isDmnElementType(elem, InputImpl.class)
					&& this.dmn.getInputVariableType((Input) elem).endsWith("formula")) {
				addDecisionTableMethodInputVariable(elem, code);
			}
		}
	}

	@Override
	public void addDecisionTableMethodOutputVariable(final DmnTemplate code) {
		String outputVariableType = this.dmn.getTypeRefToReturn(this.dmn.getCurrentDecisionTable(),
				this.dmnEngine);
		final String outputVariableName = this.dmn
				.getOutputVariableName(this.dmn.getCurrentDecisionTable());
		String initation = "";
		switch (DmnUtils.getDecisionTableType(this.dmn.getCurrentDecisionTable())) {
		case "11":
			if (outputVariableType.equals("boolean")) {
				initation = "false";
			} else if (outputVariableType.equals("double")) {
				initation = "0.0";
			} else if (outputVariableType.equals("long")) {
				initation = "0L";
			} else if (outputVariableType.equals("int")) {
				initation = "0";
			} else {
				initation = "null";
			}
			break;
		case "1n":
			initation = "new ".concat(outputVariableType).concat("()");
			break;
		case "n1":
			initation = (this.dmnEngine == DmnEngine.camunda ? "Variables.createVariables"
					: "new Hash".concat(outputVariableType)).concat("()");
			break;
		case "nn":
			final String decisionId = DmnUtils
					.namingConvention(this.dmn.getCurrentDecision().getId(), "intern");
			outputVariableType = "final ArrayList<Output".concat(decisionId).concat(">");
			initation = "new ArrayList<>()";
			code.addDecisionTableMethodOutputVariable(outputVariableType, "outputList", initation);
			outputVariableType = "String";
			initation = "\"\"";
		}
		code.addDecisionTableMethodOutputVariable(outputVariableType, outputVariableName,
				initation);
	}

	@Override
	public void addDecisionTableMethodRules(final List<DmnElement> elems, final DmnTemplate code) {
		List<Rule> rules = new ArrayList<>(this.dmn.getCurrentDecisionTable().getRules());
		final HitPolicy hitPolicy = this.dmn.getCurrentDecisionTable().getHitPolicy();
		if ((hitPolicy == HitPolicy.PRIORITY) || (hitPolicy == HitPolicy.OUTPUT_ORDER)) {
			final List<Rule> prioritizedRules = new ArrayList<>();
			prioritizeRules(prioritizedRules, rules, 0);
			rules = prioritizedRules;
		}
		for (int rule = 0; rule < rules.size(); rule++) {
			code.addDecisionTableMethodRuleCondition(rules.get(rule), elems, rule == 0);
			code.addDecisionTableMethodRuleOutput(rules.get(rule));
		}
	}

	@Override
	public void addMainClass(final DmnTemplate code) {
		code.addMainClassHeader();
		code.addExecuteMethod();
		for (final DrgElement drgElement : this.dmn.getDefinitions().getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				this.dmn.setCurrentDecision((Decision) drgElement);
				if (DmnUtils.isDmnElementType(this.dmn.getCurrentDecision().getExpression(),
						DecisionTableImpl.class)) {
					this.dmn.setCurrentDecisionTable(
							((DecisionTable) this.dmn.getCurrentDecision().getExpression()));
					addDecisionTableMethod(code);
				}
			}
		}
		code.addLine("}");
	}

	@Override
	public void addOutputClasses(final DmnTemplate code) {
		for (final DecisionTable decisionTable : this.dmn.getAllDecisionTables()) {
			if (DmnUtils.getDecisionTableType(decisionTable).equals("nn")) {
				code.addOutputClass(decisionTable);
			}
		}
	}

	@Override
	public void checkForErrors() {
		if (this.dmn.getMainDecision() == null) {
			System.err.println("ERROR: The dmn-file could not be read correctly.\n"
					.concat("The main decision, delivering the result, could not be found.\n")
					.concat("Please correct the dmn-file and try again."));
			System.exit(-1);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void prioritizeRules(final List<Rule> prioritizedRules, final List<Rule> rulesLeft,
			final int outputNr) {
		@SuppressWarnings("rawtypes")
		final List<Output> outputs = new ArrayList(this.dmn.getCurrentDecisionTable().getOutputs());
		if (outputNr < outputs.size()) {
			final Output output = outputs.get(outputNr);
			if (output.getOutputValues() != null) {
				final String priorityOrder = output.getOutputValues().getTextContent();
				final List<String> priorities = Arrays.asList(priorityOrder.split(","));
				for (final String priority : priorities) {
					final List<Rule> matches = new ArrayList<>();
					for (int ruleNr = 0; ruleNr < rulesLeft.size(); ruleNr++) {
						final Rule rule = rulesLeft.get(ruleNr);
						final ArrayList<OutputEntry> outputEntries = new ArrayList<>(
								rule.getOutputEntries());
						if (outputEntries.get(outputNr).getTextContent().equals(priority)) {
							matches.add(rulesLeft.get(ruleNr));
						}
					}
					if ((matches.size() == 1) || (outputNr == (outputs.size() - 1))) {
						prioritizedRules.addAll(matches);
					} else {
						prioritizeRules(prioritizedRules, matches, outputNr + 1);
					}
				}
			} else {
				prioritizeRules(prioritizedRules, rulesLeft, outputNr + 1);
			}
		} else {
			prioritizedRules.addAll(rulesLeft);
		}
	}

	@Override
	public List<String> transform(final String outputPackage) {
		return transform(outputPackage, DmnEngine.standAlone);
	}

	@Override
	public List<String> transform(final String outputPackage, final DmnEngine dmnEngine) {
		this.dmnEngine = dmnEngine;
		final DmnTemplate code = new DmnTemplateImpl(this.dmn, this.dmnEngine);
		checkForErrors();
		code.addJavaFileImports(outputPackage);
		addMainClass(code);
		addOutputClasses(code);
		return code.getJavaFile();
	}
}
