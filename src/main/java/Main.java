import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnTools;
import de.materna.dmntools.impl.DmnToolsImpl;

public class Main {

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
	}
}