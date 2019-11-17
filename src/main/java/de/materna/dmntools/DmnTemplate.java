package de.materna.dmntools;

import java.util.List;

import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Rule;

public interface DmnTemplate {

	void addCamundaExecuteMethodGetVariables();

	void addCamundaExecuteMethodMainMethodCall();

	void addCamundaExecuteMethodSetVariable();

	void addDecisionTableMethodFooter();

	void addDecisionTableMethodHeader(final String methodName);

	void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String source);

	void addDecisionTableMethodInputVariable(final String typeRef, final String variableName,
			final String requiredDecisionId, final DecisionTable decisionTable);

	void addDecisionTableMethodOutputVariable(final String variableType, final String variableName,
			final String initiation);

	void addDecisionTableMethodRuleCondition(final Rule rule, final List<DmnElement> elems,
			final boolean firstRule);

	void addDecisionTableMethodRuleOutput(final Rule rule);

	void addEmptyLine();

	void addExecuteMethod();

	void addJavaFileImports(final String outputPackage);

	void addLine(final String line);

	void addMainClassHeader();

	void addOutputClass(final DecisionTable decisionTable);

	void addStringBuilder(final StringBuilder sb);

	String buildCondition(final Input input, final String match);

	String buildConditionPart(final Input input, final String elem);

	List<String> getJavaFile();
}