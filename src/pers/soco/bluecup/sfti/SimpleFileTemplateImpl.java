package pers.soco.bluecup.sfti;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import pers.soco.bluecup.util.Util;

/**
 * Simple file template implemention.This class generates directories and files
 * according to a template. Strings like ${paramName} in file name and file
 * content of the template are variables,use map.put('paramName','paramValue')
 * to set the values of them,call gen() method to generate what is described in
 * the template.
 * 
 * @author RP_S
 * @since 2016.10.26
 */
public class SimpleFileTemplateImpl {
	public static final String DEFAULT_CHARSET = "utf-8";
	private static String CURRENT_CHARSET = DEFAULT_CHARSET;

	private static String TEMPLATE_ROOT;

	public static final String DEFAULT_OUT_PATH = System.getProperty("user.dir") + "/sfti/";
	private static String CURRENT_OUT_PATH = DEFAULT_OUT_PATH;

	public static final Pattern PATTERN = Pattern.compile("\\$\\{[\\w_]+\\}");

	public static void gen(Map<String, String> params, File template, String outPath, String charset,
			boolean cleanOutFolder) throws IOException {
		if (charset == null || charset.length() == 0) {
			CURRENT_CHARSET = charset;
		} else {
			CURRENT_CHARSET = charset;
		}

		if (outPath == null || outPath.length() == 0) {
			CURRENT_OUT_PATH = DEFAULT_OUT_PATH;
		} else {
			CURRENT_OUT_PATH = outPath;
		}

		File outFolder = new File(CURRENT_OUT_PATH);
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}

		if (!template.exists()) {
			throw new FileNotFoundException("Template file not found");
		}

		if (cleanOutFolder) {
			Util.delete(new File(DEFAULT_OUT_PATH));
		}

		if (template.isDirectory()) {
			TEMPLATE_ROOT = template.getAbsolutePath();
			File[] files = template.listFiles();
			if (files != null) {
				for (File f : files) {
					gen(params, f);
				}
			}
		} else {
			TEMPLATE_ROOT = template.getParentFile().getAbsolutePath();
			gen(params, template);
		}
	}

	private static void gen(Map<String, String> params, File templateFile) throws IOException {
		if (templateFile == null || !templateFile.exists()) {
			return;
		}

		String fileName = templateFile.getPath();

		fileName = fileName.replace(TEMPLATE_ROOT, "");

		String path = CURRENT_OUT_PATH + Util.replaceAllParams(params, fileName, PATTERN);

		File newFile = new File(path);

		if (templateFile.isDirectory()) {
			newFile.mkdirs();
		} else {
			// do not override files in outpath
			if (!newFile.exists()) {
				File parentFile = newFile.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				newFile.createNewFile();

				String content = Util.read(templateFile, CURRENT_CHARSET);
				content = Util.replaceAllParams(params, content, PATTERN);

				Util.write(newFile, content);
			}
		}

		if (templateFile.isDirectory()) {
			File[] files = templateFile.listFiles();
			for (File file : files) {
				gen(params, file);
			}
		}
	}
}
