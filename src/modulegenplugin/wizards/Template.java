package modulegenplugin.wizards;

import java.io.File;

/**
 * 封装插件模版包中的一个模块，便于获取配置文件路径和真正的模版文件路径等
 * 
 * @author RP_S
 * @since 2016.10.27
 */
public class Template {
	// 模块包的根目录
	private String root;
	private String moduleName;

	// TODO 配置文件，需要配置更多东西。从配置文件读取，需要声称模版时，手动输入的东西
	public Template(String moduleFolder) {
		root = moduleFolder;
		moduleName = root.substring(root.lastIndexOf(File.separator) + 1);
	}

	/**
	 * 返回模版文件
	 * 
	 * @return
	 */
	public File getTemplate() {
		File file = new File(root + "/template/");
		if (file.exists()) {
			return file;
		}
		throw new RuntimeException("Template file of module 【" + moduleName + "】not found");
	}

	/**
	 * 获取配置文件
	 * 
	 * @return
	 */
	public File getConfigFile() {
		File file = new File(root + "/params.txt");
		return file;
	}
}
