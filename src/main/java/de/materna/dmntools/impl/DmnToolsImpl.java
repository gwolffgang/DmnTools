package de.materna.dmntools.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import de.materna.dmntools.DmnEngine;
import de.materna.dmntools.DmnMetaModel;
import de.materna.dmntools.DmnStructure;
import de.materna.dmntools.DmnTools;
import de.materna.dmntools.DmnUtils;

public class DmnToolsImpl implements DmnTools {

	public static File outputDirectory = null;
	public static String outputPackage = "";
	public static File resourcesDirectory = null;

	@Override
	public void checkForErrors() {
		if (outputPackage.contentEquals("")) {
			consoleText("error", "Package of the java file not set.\n"
					.concat("Please use method 'setOutputPackage(String)'."));
			System.exit(-2);
		}
		if (outputDirectory == null) {
			consoleText("error",
					"Output directory for the generated java files not set.\n"
							.concat("Please use methods 'setOutputDirectory(File)' ")
							.concat("or 'setOutputDirectory(String)'."));
			System.exit(-3);
		}
		if (resourcesDirectory == null) {
			consoleText("error",
					"Resources directory of the dmn-files to transform not set.\n"
							.concat("Please use methods 'setResourcesDirectory(File)' ")
							.concat("or 'setResourcesDirectory(String)'."));
			System.exit(-4);
		} else {
			if (DmnUtils.getDmnFilesList(resourcesDirectory).isEmpty()) {
				consoleText("error", "Chosen resources directory does not contain *.dmn files.\n"
						.concat("Please add *.dmn files or use methods ")
						.concat("'setResourcesDirectory(File)' or ")
						.concat("'setResourcesDirectory(String)' to change the directory."));
				System.exit(-5);
			}
		}
	}

	@Override
	public void consoleText(final String tag) {
		consoleText(tag, "");
	}

	@Override
	public void consoleText(final String tag, final String text) {
		switch (tag) {
		case "error":
			System.err.println("ERROR: ".concat(text));
			break;
		case "start":
			System.out.println("");
			System.out.println("Writing in ".concat(getOutputDirectory()).concat(":"));
			break;
		case "read":
			System.out.print(" * Reading ".concat(text.substring(text.indexOf("/") + 1)));
			break;
		case "write":
			System.out.print(" * Writing ".concat(text));
			break;
		case "finished":
			System.out.println(" (finished)");
			break;
		case "end":
			System.out.println("");
			System.out.println("All done. Have a nice day!");
			System.out.println("");
		}
	}

	@Override
	public void dmn2Java(final File source, final DmnEngine dmnEngine) {
		checkForErrors();
		consoleText("read", source.getPath());
		final DmnMetaModel dmn = new DmnMetaModelImpl(source);
		consoleText("finished");
		final DmnStructure structure = new DmnStructureImpl(dmn);
		final List<String> javaFile = structure.transform(outputPackage, dmnEngine);
		final StringBuilder javaFileName = new StringBuilder();
		javaFileName.append("Dmn");
		javaFileName.append(DmnUtils.namingConvention(dmn.getDefinitions().getId(), "intern"));
		javaFileName.append(dmnEngine == DmnEngine.camunda ? "Delegate" : "");
		javaFileName.append(".java");
		writeJavaClass(javaFile, javaFileName.toString());
	}

	@Override
	public void dmn2Java(final String source) {
		dmn2Java(new File(resourcesDirectory + "/" + source), DmnEngine.standAlone);
	}

	@Override
	public void dmn2JavaAll() {
		dmn2JavaAll(DmnEngine.standAlone);
	}

	@Override
	public void dmn2JavaAll(final DmnEngine dmnEngine) {
		consoleText("start");
		final List<File> dmnSources = DmnUtils.getDmnFilesList(resourcesDirectory);
		for (final File source : dmnSources) {
			dmn2Java(source, dmnEngine);
		}
		consoleText("end");
	}

	@Override
	public String getOutputDirectory() {
		return outputDirectory.getPath();
	}

	@Override
	public String getOutputPackage() {
		return outputPackage;
	}

	@Override
	public String getResourcesDirectory() {
		return resourcesDirectory.getPath();
	}

	@Override
	public void setOutputDirectory(final File outputDirectory) {
		DmnToolsImpl.outputDirectory = outputDirectory;
	}

	@Override
	public void setOutputDirectory(final String outputDirectory) {
		setOutputDirectory(new File(outputDirectory));
		DmnToolsImpl.outputDirectory.mkdirs();
	}

	@Override
	public void setOutputPackage(final String outputPackage) {
		DmnToolsImpl.outputPackage = outputPackage;
	}

	@Override
	public void setResourcesDirectory(final File resourcesDirectory) {
		DmnToolsImpl.resourcesDirectory = resourcesDirectory;
	}

	@Override
	public void setResourcesDirectory(final String resourcesDirectory) {
		setResourcesDirectory(new File(resourcesDirectory));
	}

	@Override
	public void writeJavaClass(final List<String> code, final String fileName) {
		consoleText("write", fileName);
		if (code != null) {
			final Path saveLocation = Paths.get(getOutputDirectory().concat("/").concat(fileName));
			try (OutputStream stream = Files.newOutputStream(saveLocation);
					OutputStream bos = new BufferedOutputStream(stream);
					PrintStream ps = new PrintStream(bos);) {
				for (final String line : code) {
					ps.append(line.concat("\n"));
				}
			} catch (final IOException e) {
				System.err.format("IOException: %s%n", e);
				System.exit(-1);
			}
		}
		consoleText("finished");
	}
}
