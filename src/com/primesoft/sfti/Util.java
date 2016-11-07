package com.primesoft.sfti;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Util {

	public static String read(File file, String charset) throws IOException {
		if (file == null || !file.exists() || file.isDirectory()) {
			return null;
		}
		return read(new FileInputStream(file), charset);
	}

	public static String read(InputStream is, String charset) throws IOException {
		if (is == null) {
			return null;
		}
		try {
			byte[] buff = new byte[1024];
			byte[] all = null;
			byte[] temp = null;
			int n = 0;
			int total = 0;
			while ((n = is.read(buff)) != -1) {
				// 已经读取的字节总数
				total += n;
				// 第一次进来all为空，将读取的所有字节复制到all中
				if (all == null) {
					all = new byte[n];
					System.arraycopy(buff, 0, all, 0, n);
				}
				// 由于扩容all会丢失all的内容，所以先将已经读取的所有字节保存到temp中
				temp = all;
				// 将all扩容
				all = new byte[total];
				// 将保存到temp中的部分复制到前total-n个位置
				System.arraycopy(temp, 0, all, 0, total - n);
				// 将新读取的部分复制到后n个位置
				System.arraycopy(buff, 0, all, total - n, n);
			}
			return new String(all, charset);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(is);
		}
		return null;
	}

	public static void write(File file, String content, String charset) throws IOException {
		if (file == null || !file.exists() || file.isDirectory()) {
			return;
		}
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file, true), charset);
			writer.write(content);
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(writer);
		}
	}

	public static void delete(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				delete(f);
			}
		}
		file.delete();
	}

	public static String replaceAllParams(Map<String, String> params, String s, Pattern pattern) {
		if (params == null || s == null || pattern == null) {
			return s;
		}
		if (!matches(pattern, s)) {
			return s;
		}
		Iterator<String> it = params.keySet().iterator();
		while (it.hasNext()) {
			String k = it.next();
			String v = params.get(k);
			if (v == null) {
				continue;
			}
			String regex = "\\$\\{" + k + "\\}";
			try {
				s = s.replaceAll(regex, v);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return s;
	}

	public static boolean matches(Pattern p, String s) {
		if (s == null || p == null) {
			return false;
		}
		try {
			Matcher m = p.matcher(s);
			while (m.find()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void unZip(File file, String des) throws IOException {
		if (file == null || !file.exists()) {
			return;
		}

		InputStream is = null;
		OutputStream os = null;
		ZipFile zipFile = null;

		try {
			if (!(des.endsWith("\\") || des.endsWith("/"))) {
				des += "/";
			}

			new File(des).mkdirs();

			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> enu = zipFile.entries();
			while (enu.hasMoreElements()) {
				ZipEntry entry = enu.nextElement();
				is = zipFile.getInputStream(entry);

				String entryFileName = des + entry.getName();
				entryFileName = entryFileName.replaceAll("\\*", "/");
				File entryFile = new File(entryFileName);

				if (entry.isDirectory()) {
					entryFile.mkdirs();
				} else {
					if (entryFile.exists()) {
						entryFile.delete();
					}

					File pFile = entryFile.getParentFile();
					if (!pFile.exists()) {
						pFile.mkdirs();
					}
					entryFile.createNewFile();

					os = new FileOutputStream(entryFile);
					byte[] buff = new byte[1024];
					int n = -1;
					while ((n = is.read(buff)) != -1) {
						os.write(buff, 0, n);
					}
					os.close();
				}
				close(is);
			}
			close(zipFile);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(is);
			close(os);
			close(zipFile);
		}
	}

	public static void close(Closeable o) {
		try {
			if (o != null) {
				o.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(InputStream is, File des) {
		OutputStream os = null;
		try {
			os = new FileOutputStream(des);
			byte[] buff = new byte[1024];
			int n = -1;
			while ((n = is.read(buff)) != -1) {
				os.write(buff, 0, n);
			}
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(is);
			close(os);
		}
	}

	public static String getCaptial(String s) {
		StringBuilder result = new StringBuilder();
		char[] chs = s.toCharArray();
		for (char ch : chs) {
			if (Character.isUpperCase(ch)) {
				result.append(ch);
			}
		}
		return result.toString();
	}
}
