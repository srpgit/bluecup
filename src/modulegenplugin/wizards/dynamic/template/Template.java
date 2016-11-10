package modulegenplugin.wizards.dynamic.template;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.primesoft.sfti.Util;

/**
 * 将模版包中的一个模块的目录结构及配置文件规则封装起来
 * 
 * @author RP_S
 * @since 2016.10.30
 */
public class Template {
	// 模块包的根目录
	private String root;
	private String moduleName;
	private List<InputParam> inputParmas;
	private Map<String, String> defineParams;
	private File templateFile;
	private File configFile;

	public Template(String moduleFolder) {
		root = moduleFolder;
		moduleName = root.substring(root.lastIndexOf(File.separator) + 1);
		inputParmas = new ArrayList<InputParam>();
		defineParams = new HashMap<String, String>();

		getTemplate();
		File config = getConfigFile();
		if (config != null && config.exists()) {
			try {
				JSONObject jo = JSONObject.parseObject(Util.read(config, "utf-8"));
				if (jo == null) {
					return;
				}
				JSONArray inputParams = jo.getJSONArray("input_params");
				if (inputParams == null) {
					return;
				}

				for (int i = 0; i < inputParams.size(); i++) {
					JSONObject p = inputParams.getJSONObject(i);
					InputParam param = new InputParam();
					param.setName(p.getString("name"));
					param.setRegex(p.getString("regex"));
					param.setTip(p.getString("tip"));
					inputParmas.add(param);
				}

				JSONObject definedParams = jo.getJSONObject("define_params");
				if (definedParams == null) {
					return;
				}
				Iterator<String> it = definedParams.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					defineParams.put(key, definedParams.getString(key));
				}
			} catch (Exception e) {
				throw new RuntimeException("Read config file of module 【" + moduleName + "】failed");
			}
		}
	}

	/**
	 * 返回模版文件
	 * 
	 * @return
	 */
	public File getTemplate() {
		if (templateFile != null && templateFile.exists()) {
			return templateFile;
		}
		return new File(root + "/template/");
	}

	/**
	 * 获取配置文件
	 * 
	 * @return
	 */
	public File getConfigFile() {
		if (configFile != null && configFile.exists()) {
			return configFile;
		}
		return new File(root + "/config.json");
	}

	public List<InputParam> getInputParams() {
		return inputParmas;
	}

	public Map<String, String> getDefineParams() {
		return defineParams;
	}
}
