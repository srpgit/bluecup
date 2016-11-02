package com.primesoft.sfti;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 简单文件模版的实现。 根据模版生成文件目录。 使用${paramName}在模版文件的文件名和文件内容中占位。
 * map.put('paramName','paramValue')设置对应的变量值。 调用ModuleGen.gen(map)生成文件目录
 * 
 * @author RP_S
 * @since 2016.10.26
 */
public class SimpleFileTemplateImpl {
	public static final String DEFAULT_CHARSET = "utf-8";
	private static String CURRENT_CHARSET = DEFAULT_CHARSET;

	private static String TEMPLATE_ROOT;

	public static final String DEFAULT_OUT_PATH = System.getProperty("user.dir") + "/modulegen/";
	private static String CURRENT_OUT_PATH = DEFAULT_OUT_PATH;

	public static final Pattern PATTERN = Pattern.compile("\\$\\{[\\w_]+\\}");

	/**
	 * 使用自定义的目录和编码生成。为空则使用默认值
	 * 
	 * @param params
	 *            参数map
	 * @param template
	 *            模版文件。若该文件是一个文件，则从这个文件开始。若是一个文件夹，则从其所有子文件夹开始
	 * @param outPath
	 *            输出路径，绝对路径。一定是一个文件夹
	 * @param charset
	 *            字符集
	 * @param cleanOutFolder
	 *            生成时是否清空输出目录
	 * @throws IOException
	 */
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
			throw new FileNotFoundException("模版文件不存在");
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
			// 若输出目录的文件已存在，则不覆盖
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
