package modulegenplugin.wizards;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import com.alibaba.fastjson.JSONArray;
import com.primesoft.sfti.SimpleFileTemplateImpl;
import com.primesoft.sfti.Util;

/**
 * The "New" wizard page allows setting the container for the new file as well as the file name. The
 * page will only accept file name without the extension OR with the extension that matches the
 * expected one (mpe).
 */

public class DynamicTemplatePage extends WizardPage implements IModulePage {
	private final String templatesFile = "templates.zip";

	private Text containerText;

	private Text moduleName;

	private Text externalTemplatesText;

	private Map<String, Template> externalemplates;

	private Button useExternal;

	private Combo combo;

	private Map<String, Template> templates;

	private ISelection selection;

	private static ImageDescriptor img = ImageDescriptor.createFromURL(Thread.currentThread().getContextClassLoader().getResource("icon.png"));

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public DynamicTemplatePage(ISelection selection) {
		super("");
		setTitle("Dynamic Template Module Generator");
		setDescription("Generate a new module or file system by the selected template ");
		setImageDescriptor(img);
		this.selection = selection;
		this.templates = new HashMap<String, Template>();
		this.externalemplates = new HashMap<String, Template>();
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Parent Folder:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		containerText.setEditable(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Module Name:");

		moduleName = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		moduleName.setLayoutData(gd);
		moduleName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);

		useExternal = new Button(container, SWT.CHECK);
		useExternal.setText("External Templates:");

		externalTemplatesText = new Text(container, SWT.BORDER | SWT.SINGLE);
		externalTemplatesText.setEditable(false);
		externalTemplatesText.setEnabled(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		externalTemplatesText.setLayoutData(gd);
		externalTemplatesText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		final FileDialog fileDialog = new FileDialog(container.getShell(), SWT.OPEN);
		fileDialog.setFilterExtensions(new String[] { "templates.zip", "*.zip" });
		final Button extButton = new Button(container, SWT.PUSH);
		extButton.setText("Browse...");
		extButton.setEnabled(false);
		extButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fileName = fileDialog.open();
				if (fileName != null) {
					externalTemplatesText.setText(fileName);
					loadExternalTemplates();
				}
			}
		});

		useExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (useExternal.getSelection()) {
					externalTemplatesText.setEnabled(true);
					extButton.setEnabled(true);
					if (externalTemplatesText.getText().length() > 0) {
						loadExternalTemplates();
					}
				} else {
					externalTemplatesText.setEnabled(false);
					extButton.setEnabled(false);
					externalemplates.clear();
					refreshCombo();
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Template:");

		combo = new Combo(container, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});

		initialize();
		loadTemplates(this.templates, Thread.currentThread().getContextClassLoader().getResourceAsStream(templatesFile), "default");
		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();

			String text = "";
			if (obj instanceof IPackageFragment) {
				IPackageFragment ipf = (IPackageFragment) obj;
				text = ipf.getPath().makeAbsolute().toString();
			} else if (obj instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot ipf = (IPackageFragmentRoot) obj;
				text = ipf.getPath().makeAbsolute().toString();
			} else if (obj instanceof IResource) {
				IResource ir = (IResource) obj;
				if (ir instanceof IFolder) {
					text = ir.getFullPath().toString();
				} else {
					text = ir.getParent().getFullPath().toString();
				}
			}

			containerText.setText(text);
		}
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select Parent Folder");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		String moduleName = getModuleName();

		if (getContainerName().length() == 0) {
			updateStatus("Parent folder must be specified");
			return;
		}
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Parent folder must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (moduleName.length() == 0) {
			updateStatus("Module name must be specified");
			return;
		}
		if (!Pattern.matches("[\\w]+", moduleName)) {
			updateStatus("Module name must only contain letter or digit");
			return;
		}
		if (!Character.isLetter(moduleName.charAt(0))) {
			updateStatus("Module name must start with letter");
			return;
		}
		if (Character.isLowerCase(moduleName.charAt(0))) {
			updateStatus("The first letter of module name must be uppercase");
			return;
		}

		Template template = getTemplate();
		if (template == null) {
			updateStatus("Selected tempalte not exists");
			return;
		}

		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(templatesFile);
		if (is == null) {
			updateStatus("Template file : " + templatesFile + " not found");
			return;
		}
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		updateStatus(message, message == null);

	}

	private void updateStatus(String message, boolean complete) {
		setErrorMessage(message);
		setPageComplete(complete);
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getModuleName() {
		return moduleName.getText();
	}

	@Override
	public Map<String, String> getParams() throws IOException {
		Template template = getTemplate();
		if (template == null) {
			throw new RuntimeException("No tempalte is selected");
		}

		String containerName = getContainerName();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));

		String path = resource.getFullPath().toString();

		// package name starts from header
		List<Object> headers = new ArrayList<Object>();
		// read from config.json
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");
		// if config file not exists,user defaults
		if (is == null) {
			headers.add("/src/main/java/");
			headers.add("/src/main/java");
			headers.add("/src/");
			headers.add("/src");
		} else {
			headers = JSONArray.parseArray(Util.read(is, "utf-8"));
		}

		for (Object header : headers) {
			String headerStr = String.valueOf(header);
			int index = path.indexOf(headerStr);
			if (index != -1) {
				path = path.substring(index + headerStr.length());
				break;
			}
		}

		String parentPackege = path.replace("/", ".");
		if (parentPackege.length() > 0) {
			parentPackege += ".";
		}

		String ModuleName = getModuleName();

		String firstLower = ModuleName.substring(0, 1).toLowerCase() + ModuleName.substring(1);

		// system variables
		Map<String, String> systemVars = new HashMap<String, String>();

		systemVars.put("ModuleName", ModuleName);
		systemVars.put("moduleName", firstLower);
		systemVars.put("modulename", ModuleName.toLowerCase());
		systemVars.put("MODULENAME", ModuleName.toUpperCase());
		systemVars.put("MN", Util.getCaptial(ModuleName));
		systemVars.put("mn", systemVars.get("MN").toLowerCase());
		systemVars.put("parentpackage", parentPackege);
		systemVars.put("package", parentPackege + systemVars.get("modulename"));

		// result params
		Map<String, String> params = new HashMap<String, String>();

		// replace system variables in config file
		File config = template.getConfigFile();
		if (config != null && config.exists()) {
			Properties properties = new Properties();
			properties.load(new FileInputStream(config));
			Enumeration<Object> e = properties.keys();
			Pattern pattern = SimpleFileTemplateImpl.PATTERN;
			while (e.hasMoreElements()) {
				String key = String.valueOf(e.nextElement());
				// escape keys in systemVars
				if (systemVars.containsKey(key)) {
					continue;
				}
				// escape keys like ${key}
				if (Util.matches(pattern, key)) {
					continue;
				}
				String value = properties.getProperty(key);
				// replace value
				if (Util.matches(pattern, value)) {
					value = Util.replaceAllParams(systemVars, value, pattern);
				}

				params.put(key, value);
			}
		}

		// add all systemVars to params
		Iterator<String> it = systemVars.keySet().iterator();
		while (it.hasNext()) {
			String k = it.next();
			params.put(k, systemVars.get(k));
		}

		return params;
	}

	@Override
	public Template getTemplate() {
		String k = combo.getText();
		if (k.startsWith("default")) {
			return templates.get(k);
		} else {
			return externalemplates.get(k);
		}
	}

	@Override
	public File getTempalteFile() {
		Template template = getTemplate();
		if (template != null) {
			return template.getTemplate();
		}
		return null;
	}

	@Override
	public String getOutAbsolutePath() {
		String containerName = getContainerName();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		String absolutePath = resource.getLocationURI().getPath();
		return absolutePath;
	}

	@Override
	public String getRefreshContainer() {
		return getContainerName();
	}

	private void loadTemplates(Map<String, Template> map, InputStream is, String namespace) {
		if (is == null) {
			return;
		}
		String tempPath = System.getProperty("java.io.tmpdir");
		if (tempPath == null || tempPath.length() == 0) {
			return;
		}

		tempPath = tempPath + "/modulegen/" + namespace + "/";
		File tempFolder = new File(tempPath);

		Util.delete(tempFolder);// 清空临时目录

		if (!tempFolder.exists()) {// 重建临时目录
			tempFolder.mkdirs();
		}

		File tempFile = new File(tempPath + "dyncmic_template_module.tmp");

		try {
			tempFile.createNewFile();
			Util.write(is, tempFile); // 创建临时文件
			Util.unZip(tempFile, tempPath);// 解压临时文件
			tempFile.delete();// 删除临时文件
		} catch (IOException e) {
			e.printStackTrace();
		}

		File tf = new File(tempPath + "/templates/");

		// 将templates下的每个子级文件夹视为一个模块
		File[] files = tf.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		if (files != null && files.length > 0) {
			for (File file : files) {
				map.put(namespace + "-> " + file.getName(), new Template(file.getAbsolutePath()));
			}
		}
		refreshCombo();
	}

	private void refreshCombo() {
		combo.removeAll();

		combo.add("--choose a template--");

		Iterator<String> it = templates.keySet().iterator();
		while (it.hasNext()) {
			combo.add(it.next());
		}

		it = externalemplates.keySet().iterator();
		while (it.hasNext()) {
			combo.add(it.next());
		}

		if (!templates.isEmpty() || externalemplates.isEmpty()) {
			combo.select(1);
		} else {
			combo.select(0);
		}
	}

	private void loadExternalTemplates() {
		externalemplates.clear();
		refreshCombo();
		String fileName = externalTemplatesText.getText();
		File file = new File(fileName);
		if (file.exists()) {
			try {
				loadTemplates(externalemplates, new FileInputStream(file), "external");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				updateStatus("Load template file : " + fileName + " failed", true);
			}
		} else {
			updateStatus("Template file : " + fileName + " not found", true);
		}

	}
}