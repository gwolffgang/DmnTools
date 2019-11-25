package de.materna.dmntools.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.instance.DecisionImpl;
import org.camunda.bpm.model.dmn.impl.instance.DecisionTableImpl;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DrgElement;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnStructure;
import de.materna.dmntools.DmnTemplate;
import de.materna.dmntools.DmnUtils;

public class DmnStructureImpl implements DmnStructure {
	private final DmnMetaModel dmn;

	private DmnEngine dmnEngine;

	public DmnStructureImpl(final DmnMetaModel dmn) {
		this.dmn = dmn;
	}

	@Override
	public void addDecisionTableMethod(final Decision decision, final DmnTemplate code) {
		code.addDecisionTableMethodHeader(decision);
		addDecisionTableMethodSubDecisionCalls(decision, code);
		addDecisionTableMethodInputVariables(decision, code);
		addDecisionTableMethodCollectVariables(decision, code);
		addDecisionTableMethodRules(decision, code);
		code.addDecisionTableMethodFooter(decision);
	}

	@Override
	public void addDecisionTableMethodCollectVariables(final Decision decision,
			final DmnTemplate code) {
		if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
			final DecisionTable table = (DecisionTable) decision.getExpression();
			if ((table.getHitPolicy() == HitPolicy.COLLECT) && (table.getAggregation() != null)) {
				switch (table.getAggregation()) {
				case MIN:
					code.addLine("\t\tdouble result = Double.MAX_VALUE;");
					break;
				case MAX:
					code.addLine("\t\tdouble result = Double.MIN_VALUE;");
					break;
				case SUM:
				case COUNT:
					code.addLine("\t\tdouble result = 0.0;");
				}
			}
		}
	}

	@Override
	public void addDecisionTableMethodInputVariable(final Decision decision, final Input input,
			final DmnTemplate code) {
		final InputExpression inputExpression = input.getInputExpression();
		final String inputExpressionText = inputExpression.getTextContent().isEmpty()
				? input.getCamundaInputVariable()
				: inputExpression.getTextContent();
		String typeRef = DmnUtils.getJavaVariableType(inputExpression.getTypeRef());
		String variableName = this.dmn.getInputVariableName(input, "component");
		String source = "(".concat(typeRef).concat(") new FEELImpl().evaluate(\"")
				.concat(inputExpressionText).concat("\", inputVariables)");
		if (!DmnUtils.getInputVariableType(input).endsWith("empty")
				&& (typeRef.equals("double") || typeRef.equals("long") || typeRef.equals("int"))) {
			source = "new BigDecimal(new FEELImpl().evaluate(\"".concat(inputExpressionText)
					.concat("\", inputVariables).toString());\n".concat("\t\t").concat(typeRef)
							.concat(" ").concat(variableName).concat(" = "));
			variableName += "Big";
			source += variableName.concat("." + typeRef + "Value()");
			typeRef = "BigDecimal";
		}
		code.addDecisionTableMethodInputVariable(typeRef, variableName, source);
	}

	@Override
	public void addDecisionTableMethodInputVariables(final Decision decision,
			final DmnTemplate code) {
		final List<Input> inputs = this.dmn.getInputs(false, true, true, decision);
		for (final Input input : inputs) {
			if (!DmnUtils.getInputVariableType(input).endsWith("formula")) {
				addDecisionTableMethodInputVariable(decision, input, code);
			}
		}
		for (final Input input : inputs) {
			if (DmnUtils.getInputVariableType(input).endsWith("formula")) {
				addDecisionTableMethodInputVariable(decision, input, code);
			}
		}
		if (DmnUtils.getDecisionTableType((DecisionTable) decision.getExpression()).contains("n")) {
			code.addDecisionTableMethodVariableMaps(decision);
		}
	}

	@Override
	public void addDecisionTableMethodRules(final Decision decision, final DmnTemplate code) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		final List<Input> inputs = this.dmn.getInputs(false, true, true, decision);

		List<Rule> rules = new ArrayList<>(table.getRules());
		final HitPolicy hitPolicy = table.getHitPolicy();
		if ((hitPolicy == HitPolicy.PRIORITY) || (hitPolicy == HitPolicy.OUTPUT_ORDER)) {
			final List<Rule> prioritizedRules = new ArrayList<>();
			prioritizeRules(decision, prioritizedRules, rules, 0);
			rules = prioritizedRules;
		}
		for (int rule = 0; rule < rules.size(); rule++) {
			code.addDecisionTableMethodRuleCondition(decision, rules.get(rule), inputs, rule == 0);
			code.addDecisionTableMethodRuleOutput(decision, rules.get(rule));
		}
	}

	@Override
	public void addDecisionTableMethodSubDecisionCalls(final Decision decision,
			final DmnTemplate code) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final List<InformationRequirement> informationRequirements = new ArrayList(
				decision.getInformationRequirements());
		for (final InformationRequirement informationRequirement : informationRequirements) {
			if (DmnUtils.isDmnElementType(informationRequirement.getRequiredDecision(),
					DecisionImpl.class)) {
				final Decision requiredDecision = informationRequirement.getRequiredDecision();
				code.addDecisionTableMethodSubDecisionCall(requiredDecision);
			}
		}
	}

	@Override
	public void addMainClass(final DmnTemplate code) {
		code.addMainClassHeader();
		code.addExecuteMethods();
		for (final DrgElement drgElement : this.dmn.getDefinitions().getDrgElements()) {
			if (DmnUtils.isDmnElementType(drgElement, DecisionImpl.class)) {
				final Decision decision = (Decision) drgElement;
				if (DmnUtils.isDmnElementType(decision.getExpression(), DecisionTableImpl.class)) {
					addDecisionTableMethod(decision, code);
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
		for (final Input input : this.dmn.getAllInputs()) {
			if (DmnUtils.getInputVariableType(input).contains("variable")
					&& input.getInputExpression().getTextContent().contains(".")) {
				final String variableName = this.dmn.getInputVariableName(input, "variable");
				for (final DecisionTable table : this.dmn.getAllDecisionTables()) {
					if (((Decision) table.getParentElement()).getId().equals(variableName)
							&& DmnUtils.getDecisionTableType(table).equals("nn")) {
						System.err.println("ERROR: Sorry, using the collected ouputs "
								.concat("of a DecisionTable with more than one output ")
								.concat("as an input in an InputExpression is not ")
								.concat("supported yet."));
						System.err.print(
								"Decision-Id: \"" + ((Decision) table.getParentElement()).getId());
						System.err.print("\"; InputExpression: \""
								+ this.dmn.getInputVariableName(input, "all"));
						System.err.println("\". Transformation aborded.");
						System.exit(-1);
					}
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void prioritizeRules(final Decision decision, final List<Rule> prioritizedRules,
			final List<Rule> rulesLeft, final int outputNr) {
		final DecisionTable table = (DecisionTable) decision.getExpression();
		@SuppressWarnings("rawtypes")
		final List<Output> outputs = new ArrayList(table.getOutputs());
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
						prioritizeRules(decision, prioritizedRules, matches, outputNr + 1);
					}
				}
			} else {
				prioritizeRules(decision, prioritizedRules, rulesLeft, outputNr + 1);
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
