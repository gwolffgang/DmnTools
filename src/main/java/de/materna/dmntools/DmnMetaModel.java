package de.materna.dmntools;

import java.util.List;

import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionRule;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.Expression;
import org.camunda.bpm.model.dmn.instance.InformationRequirement;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Variable;

public interface DmnMetaModel {

	String addInputNameToInputVariablesString(final boolean withVarType, final Input input,
			final String inputVariablesString);

	void checkInputOutputMatches(final Decision decision, final List<Input> list,
			final boolean withMatches, final boolean withNonMatches);

	boolean existsTableType(final String tableType);

	List<DecisionTable> getAllDecisionTables();

	List<Input> getAllInputs();

	List<Output> getAllOutputs();

	Definitions getDefinitions();

	DmnModelInstance getDmnModelInstance();

	String getExpressionVariableTypeRef(final Expression expression);

	List<String> getFilteredPossibleInputNames(final String expressionText);

	List<String> getFormulaInputNames(final String expressionText);

	List<Output> getFormulaOutputs(final Input input);

	List<Input> getInputs(final boolean deep, final boolean withMatches,
			final boolean withNonMatches, final Decision decision);

	String getInputVariableName(final Input input, final String part);

	String getInputVariablesString(final boolean withVarType);

	String getInputVariablesString(final boolean withVarType, final Decision decision);

	Decision getMainDecision();

	String getOutputValuesString(final DecisionRule rule);

	Variable getOutputVariable(final DecisionTable decisionTable);

	String getOutputVariableName(final Decision decision);

	String getOutputVariablesString(final boolean withVarType,
			final DecisionTable currentDecisionTable);

	List<Decision> getRequirementDecisions(final List<InformationRequirement> requirementsList);

	List<Input> getRequirementInputs(final List<InformationRequirement> requirementsList,
			final boolean withMatches, final boolean withNonMatches);

	String getTypeRefToReturn();

	boolean matchesOutput(final String inputName, final List<Output> outputs);

	void removeInputOutputMatches(final List<Input> list,
			final List<InformationRequirement> requirementsList, final boolean withMatches,
			final boolean withNonMatches);

	void setDefinitions(final Definitions definitions);

	void setDmnModelInstance(final DmnModelInstance dmnModelInstance);

	boolean usesCollection();

	boolean usesDate();

	boolean usesFormula();
}
