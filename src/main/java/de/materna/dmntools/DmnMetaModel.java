package de.materna.dmntools;

import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionRule;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.DmnElement;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Variable;

public interface DmnMetaModel {

	String addInputNameToInputVariablesString(final boolean withVarType, final Input input,
			final String inputVariablesString);

	void checkInputOutputMatches(final DecisionTable decisionTable, final List<DmnElement> list);

	List<List<String>> combineStrings(final List<String> list, final int elemsToCombine,
			final String original);

	boolean existsTableType(final String tableType);

	List<DecisionTable> getAllDecisionTables();

	String getCorrectedFormula(final Input input);

	Decision getCurrentDecision();

	DecisionTable getCurrentDecisionTable();

	Definitions getDefinitions();

	DmnModelInstance getDmnModelInstance();

	List<String> getFilteredPossibleInputNames(final String expressionText);

	List<String> getFormulaInputNames(final String expressionText);

	List<Output> getFormulaOutputs(final Input input);

	List<DmnElement> getInAndOutputs(boolean matched, boolean deep,
			final DecisionTable decisionTable);

	String getInputVariableName(final Input input);

	String getInputVariablesString(final boolean withVarType);

	String getInputVariablesString(final boolean withVarType, final DecisionTable decisionTable);

	String getInputVariableType(final Input input);

	Decision getMainDecision();

	String getOutputValuesString(final DecisionRule rule);

	Variable getOutputVariable(final DecisionTable decisionTable);

	String getOutputVariableName(final DecisionTable decisionTable);

	String getOutputVariablesString(final boolean withVarType,
			final DecisionTable currentDecisionTable);

	List<DecisionTable> getRequirementDecisionTables(
			final List<InformationRequirement> requirementsList);

	List<DmnElement> getRequirementInAndOutputs(
			final Collection<InformationRequirement> requirementsList, final boolean matched);

	String getTypeRefToReturn(final DecisionTable decisionTable, final DmnEngine dmnEngine);

	boolean matchesOutput(final String inputName, final List<Output> outputs);

	void removeInputOutputMatches(final List<DmnElement> list,
			final Collection<InformationRequirement> requirementsList);

	void removeOutputInputMatches(final List<DmnElement> list);

	void setCurrentDecision(final Decision currentDecision);

	void setCurrentDecisionTable(final DecisionTable decisionTable);

	void setDefinitions(final Definitions definitions);

	void setDmnModelInstance(final DmnModelInstance dmnModelInstance);

	boolean usesCollection(final String collectionType);

	boolean usesDate();
}
