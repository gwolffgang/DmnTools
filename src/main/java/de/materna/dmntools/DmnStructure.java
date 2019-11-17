package de.materna.dmntools;

import java.util.List;

import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.Rule;

public interface DmnStructure {

	void addDecisionTableMethod(final DmnTemplate code);

	void addDecisionTableMethodInputVariable(final DmnElement elem, final DmnTemplate code);

	void addDecisionTableMethodInputVariables(final List<DmnElement> elems, final DmnTemplate code);

	void addDecisionTableMethodOutputVariable(final DmnTemplate code);

	void addDecisionTableMethodRules(final List<DmnElement> elems, final DmnTemplate code);

	void addMainClass(final DmnTemplate code);

	void addOutputClasses(final DmnTemplate code);

	void checkForErrors();

	void prioritizeRules(final List<Rule> prioritizedRules, final List<Rule> rulesLeft,
			final int outputNr);

	List<String> transform(final String outputPackage);

	List<String> transform(final String outputPackage, final DmnEngine dmnEngine);
}