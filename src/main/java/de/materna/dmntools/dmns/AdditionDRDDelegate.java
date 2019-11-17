package de.materna.dmntools.dmns;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.util.ArrayList;

import com.google.gson.Gson;
// <!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->

public class AdditionDRDDelegate implements JavaDelegate {

	@Override
	public void execute(final DelegateExecution execution) throws Exception {
		final int einkommen = (int) execution.getVariable("einkommen");
		final String resultOfDmn = decisionAddition(einkommen);
		execution.setVariable("resultAdditionDRD", resultOfDmn);
	}

	private String decisionAddition(final int einkommen) {
		final ArrayList<OutputAddition> outputList = new ArrayList<>();

		String output = "";

		if (einkommen>5000) {
			OutputAddition result = new OutputAddition(20, "Ulrich", "violett");
			outputList.add(result);
		}
		if (einkommen>3000) {
			OutputAddition result = new OutputAddition(10, "Ulrich", "rot");
			outputList.add(result);
		}
		if (einkommen>9000) {
			OutputAddition result = new OutputAddition(40, "Gisela", "violett");
			outputList.add(result);
		}
		if (einkommen<16000) {
			OutputAddition result = new OutputAddition(-80, "Gisela", "blau");
			outputList.add(result);
		}
		if (einkommen<9000) {
			OutputAddition result = new OutputAddition(-90, "Thomas", "rot");
			outputList.add(result);
		}
		if (einkommen>9000) {
			OutputAddition result = new OutputAddition(30, "Thomas", "grün");
			outputList.add(result);
		}
		output = new Gson().toJson(outputList);
		return output;
	}
}

class OutputAddition {
	long abschlag;
	String kapitalgeber;
	String farbe;

	public OutputAddition(final long abschlag, final String kapitalgeber, final String farbe) {
		this.abschlag = abschlag;
		this.kapitalgeber = kapitalgeber;
		this.farbe = farbe;
	}
}
