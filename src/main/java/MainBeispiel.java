import java.util.HashMap;
import java.util.Map;

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnTools;
import de.materna.dmntools.dmns.DmnFinaleRisikobewertungDRD;
import de.materna.dmntools.impl.DmnToolsImpl;

public class MainBeispiel {

	public static void main(final String[] args) {
		// instantiate the DmnTools
		final DmnTools dmntools = new DmnToolsImpl();

		// give needed information
		dmntools.setOutputPackage("de.materna.dmntools.dmns");
		dmntools.setOutputDirectory("src/main/java/de/materna/dmntools/dmns");
		dmntools.setResourcesDirectory("src/main/resources");

		// transform the DMN-file(s)
		dmntools.dmn2JavaAll();
		dmntools.dmn2JavaAll(DmnEngine.camunda);

		// testing the created file(s)
		final DmnFinaleRisikobewertungDRD dmn = new DmnFinaleRisikobewertungDRD();
		final Map<String, Object> inputVariables = new HashMap<>();
		inputVariables.put("alter", 32);
		inputVariables.put("faktor", 0.71);
		inputVariables.put("mentorVorname", "Peter");
		inputVariables.put("mentorNachname", "Lustig");
		inputVariables.put("angestelltSeitMehrAlsSechsJahren", false);
		inputVariables.put("familienstand", "ledig");
		inputVariables.put("vertragsverhaeltnis", "unbefristet");
		System.out.println("The result is: " + dmn.execute(inputVariables));
	}
}