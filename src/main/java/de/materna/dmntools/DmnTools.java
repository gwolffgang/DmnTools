package de.materna.dmntools;

import java.io.File;
import java.util.List;

public interface DmnTools {

	void checkForErrors();

	void consoleText(final String tag);

	void consoleText(final String tag, final String text);

	void dmn2Java(final File source, final DmnEngine dmnEngine);

	void dmn2Java(final String source);

	void dmn2JavaAll();

	void dmn2JavaAll(final DmnEngine dmnEngine);

	String getOutputDirectory();

	String getOutputPackage();

	String getResourcesDirectory();

	void setOutputDirectory(final File outputDirectory);

	void setOutputDirectory(final String outputDirectory);

	void setOutputPackage(final String outputPackage);

	void setResourcesDirectory(final File resourcesDirectory);

	void setResourcesDirectory(final String resourcesDirectory);

	void writeJavaClass(final List<String> code, final String fileName);
}