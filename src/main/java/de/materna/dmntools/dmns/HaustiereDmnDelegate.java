package de.materna.dmntools.dmns;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class HaustiereDmnDelegate implements JavaDelegate {

	@Override
	public void execute(final DelegateExecution execution) throws Exception {
		final String tierNameDeutsch = (String) execution.getVariable("tierNameDeutsch");
		final int besitzerNummer = (int) execution.getVariable("besitzerNummer");
		final double rambaZamba = (double) execution.getVariable("rambaZamba");
		final String resultOfDmn = decisionHaustier(tierNameDeutsch, besitzerNummer, rambaZamba);
		execution.setVariable("pet", resultOfDmn);
	}

	private String decisionHaustier(final String tierNameDeutsch, final int besitzerNummer, final double rambaZamba) {
		final int tierNummer = decisionTiernummer(tierNameDeutsch);
		final String tierTyp = decisionTier(tierNameDeutsch);
		final String tierBesitzer = decisionPerson(besitzerNummer);
		final double berechnung = (tierNummer + besitzerNummer) / rambaZamba;
		String output = null;

		if (true && tierTyp.equals("dog") && tierBesitzer.equals("Tintin")) {
			output = "Snowy";
		} else if (berechnung == 2 && tierTyp.equals("dog") && tierBesitzer.equals("Obelix")) {
			output = "Idefix";
		} else if (true && tierTyp.equals("dog") && tierBesitzer.equals("Lucky Luke")) {
			output = "Rantanplan";
		} else if ((berechnung == 3 || berechnung == 4) && tierTyp.equals("cat") && (tierBesitzer.equals("Jonathan Q. Arbuckle") || tierBesitzer.equals("Jonathan Arbuckle") || tierBesitzer.equals("Jon"))) {
			output = "Garfield";
		} else if (true && tierTyp.equals("horse") && tierBesitzer.equals("Lucky Luke")) {
			output = "Jolly Jumper";
		} else if (true && tierTyp.equals("fly") && tierBesitzer.equals("Peter Lustig")) {
			output = "Fiete die Fruchtfliege";
		} else if (true && tierTyp.equals("dog") && tierBesitzer.equals("Peter Lustig")) {
			output = "Willi";
		} else if (true && tierTyp.equals("spider") && tierBesitzer.equals("Peter Lustig")) {
			output = "Sabine die Kreuzspinne";
		} else if (true && true && true) {
			output = "no match";
		}
		return output;
	}

	private String decisionTier(final String tierNameDeutsch) {
		final int tierNummer = decisionTiernummer(tierNameDeutsch);
		String output = null;

		if (tierNummer == 1) {
			output = "dog";
		} else if (tierNummer == 2) {
			output = "cat";
		} else if (tierNummer == 3) {
			output = "horse";
		} else if (tierNummer == 4) {
			output = "fly";
		} else if (tierNummer == 5) {
			output = "spider";
		} else if (true) {
			output = "robot";
		}
		return output;
	}

	private String decisionPerson(final int besitzerNummer) {
		String output = null;

		if (besitzerNummer == 1) {
			output = "Tintin";
		} else if (besitzerNummer == 2) {
			output = "Lucky Luke";
		} else if (besitzerNummer == 3) {
			output = "Obelix";
		} else if (besitzerNummer == 4) {
			output = "Jonathan Q. Arbuckle";
		} else if (besitzerNummer == 5) {
			output = "Jonathan Arbuckle";
		} else if (besitzerNummer == 6) {
			output = "Jon";
		} else if (true) {
			output = "Peter Lustig";
		}
		return output;
	}

	private int decisionTiernummer(final String tierNameDeutsch) {
		int output = 0;

		if (tierNameDeutsch.equals("Hund")) {
			output = 1;
		} else if (tierNameDeutsch.equals("Katze")) {
			output = 2;
		} else if (tierNameDeutsch.equals("Pferd")) {
			output = 3;
		} else if (tierNameDeutsch.equals("Fliege")) {
			output = 4;
		} else if (tierNameDeutsch.equals("Spinne")) {
			output = 5;
		} else if (tierNameDeutsch.equals("Roboter")) {
			output = 6;
		}
		return output;
	}
}
