package de.materna.dmntools;

import java.util.List;

import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Rule;

public interface DmnStructure {

	void addDecisionTableMethod(final Decision decision, final DmnTemplate code);

	void addDecisionTableMethodCollectVariables(final Decision decision, final DmnTemplate code);

	void addDecisionTableMethodInputVariable(final Decision decision, final Input input,
			final DmnTemplate code);

	void addDecisionTableMethodInputVariables(final Decision decision, final DmnTemplate code);

	void addDecisionTableMethodRules(final Decision decision, final DmnTemplate code);

	void addDecisionTableMethodSubDecisionCalls(final Decision decision, final DmnTemplate code);

	void addMainClass(final DmnTemplate code);

	void addOutputClasses(final DmnTemplate code);

	void checkForErrors();

	void prioritizeRules(final Decision decision, final List<Rule> prioritizedRules,
			final List<Rule> rulesLeft, final int outputNr);

	List<String> transform(final String outputPackage);

	List<String> transform(final String outputPackage, final DmnEngine dmnEngine);
}