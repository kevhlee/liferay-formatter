package com.liferay.formatter;

import com.liferay.portal.tools.java.parser.JavaParser;
import com.liferay.portal.tools.java.parser.util.FileUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import picocli.CommandLine;

/**
 * @author Kevin Lee
 */
@CommandLine.Command(
	description = "Formats Java code for Liferay codebases",
	name = "liferay-format"
)
public class CLI implements Runnable {

	public static void main(String[] args) {
		CommandLine commandLine = new CommandLine(new CLI());
		System.exit(commandLine.execute(args));
	}

	@Override
	public void run() {
		Set<File> filesToFormat = new LinkedHashSet<>();

		for (File file : files) {
			fetchJavaFiles(filesToFormat, file);
		}

		ExecutorService executorService = Executors.newFixedThreadPool(
			numThreads);

		List<Future<Void>> futures = new ArrayList<>();

		for (File file : filesToFormat) {
			futures.add(
				executorService.submit(
					() -> {
						String content = FileUtil.read(file);

						String newContent = JavaParser.parse(
							file, content, maxLineLength, false);

						if (!content.equals(newContent)) {
							FileUtil.write(file, newContent);

							System.out.printf(
								"Formatted '%s'%n", file.getPath());
						}

						return null;
					}));

			for (Future<Void> future : futures) {
				try {
					future.get();
				}
				catch (Exception exception) {
					System.err.printf("Error: %s%n", exception.getMessage());
				}
			}
		}
	}

	private void fetchJavaFiles(Collection<File> collection, File file) {
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();

			if (childFiles != null) {
				for (File childFile : childFiles) {
					fetchJavaFiles(collection, childFile);
				}
			}

			return;
		}

		String fileName = file.getName();

		if (fileName.endsWith(".java")) {
			collection.add(file);
		}
	}

	@CommandLine.Parameters(
		arity = "1..",
		description = "The files/directories to run the formatter on"
	)
	private File[] files;

	@CommandLine.Option(
		defaultValue = "80", names = "max-line-length",
		paramLabel = "max-line-length"
	)
	private int maxLineLength;

	@CommandLine.Option(
		defaultValue = "8", names = "num-threads", paramLabel = "num-threads"
	)
	private int numThreads = 8;

}