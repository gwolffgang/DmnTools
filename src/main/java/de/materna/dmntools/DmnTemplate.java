package de.materna.dmntools;

import java.util.List;

import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Rule;

public interface DmnTemplate {

	void addCamundaExecuteMethodSetVariable();

	void addDecisionTableMethodFooter(final Decision decision);

	void addDecisionTableMethodHeader(final Decision decision);

	void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String source);

	void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String requiredDecisionId, final Decision decision);

	void addDecisionTableMethodRuleCondition(final Decision decision, final Rule rule,
			final List<Input> inputs, final boolean firstRule);

	void addDecisionTableMethodRuleOutput(final Decision decision, final Rule rule);

	void addDecisionTableMethodSubDecisionCall(final Decision requiredDecision);

	void addDecisionTableMethodVariableMaps(final Decision decision);

	void addEmptyLine();

	void addExecuteMethods();

	void addJavaFileImports(final String outputPackage);

	void addLine(final String line);

	void addMainClassHeader();

	void addOutputClass(final DecisionTable decisionTable);

	void addStringBuilder(final StringBuilder sb);

	String buildCondition(final Decision decision, final Input input, final String match);

	String buildConditionPart(final Decision decision, final Input input, final String elem);

	List<String> getJavaFile();
}