package modulegenplugin.wizards.dynamic;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
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

import com.primesoft.sfti.SimpleFileTemplateImpl;
import com.primesoft.sfti.Util;

import modulegenplugin.component.TidySelection;
import modulegenplugin.wizards.MyWizardPage;
import modulegenplugin.wizards.dynamic.template.InputParam;
import modulegenplugin.wizards.dynamic.template.Template;

public class DynamicTemplatePage extends MyWizardPage {
	private Composite container;
	private final String templatesFile = "templates.zip";
	private Text containerText;
	private Text externalTemplatesText;
	private Map<String, Template> externalTemplates;
	private Button useExternal;
	private Combo combo;
	private Map<String, Template> templates;
	private Map<String, String> systemVars;
	private List<InputParamPair> inputParamPairs;

	private static ImageDescriptor img = ImageDescriptor
			.createFromURL(Thread.currentThread().getContextClassLoader().getResource("icon.png"));

	public DynamicTemplatePage(TidySelection selection) {
		super("");
		setTitle("Dynamic Template Module Generator");
		setDescription("Generate a new module or file system by the selected template ");
		setImageDescriptor(img);

		this.templates = new HashMap<String, Template>();
		this.externalTemplates = new HashMap<String, Template>();
		this.inputParamPairs = new ArrayList<InputParamPair>();
		this.selection = selection;

		systemVars = new HashMap<String, String>();
		systemVars.put("project_path", this.selection.getProjectPath());
		systemVars.put("project_name", this.selection.getProjectName());
		systemVars.put("package_name", this.selection.getPackageName());
		systemVars.put("node_path", this.selection.getProjectPath());
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
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
				ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
						ResourcesPlugin.getWorkspace().getRoot(), false, "Select Parent Folder");
				if (dialog.open() == ContainerSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					if (result.length == 1) {
						containerText.setText(((Path) result[0]).toString());
					}
				}
			}
		});

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
					externalTemplates.clear();
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
				loadInputParams();
			}
		});

		// empty
		label = new Label(container, SWT.NULL);

		loadTemplates(this.templates, Thread.currentThread().getContextClassLoader().getResourceAsStream(templatesFile),
				"default");
		loadInputParams();
		dialogChanged();
		setControl(container);
	}

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));

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

		for (InputParamPair ipp : inputParamPairs) {
			if (!Util.matches(ipp.pattern, ipp.value.getText())) {
				updateStatus(ipp.inputParam.getTip(), false);
				return;
			}
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

	@Override
	public Map<String, String> getParams() throws IOException {
		Template template = getTemplate();
		if (template == null) {
			throw new RuntimeException("No tempalte is selected");
		}

		// result params
		Map<String, String> params = new HashMap<String, String>();

		// add input params
		for (int i = 0; i < inputParamPairs.size(); i++) {
			InputParamPair ipp = inputParamPairs.get(i);
			String key = ipp.name.getText();
			String value = ipp.value.getText();
			// can override system vars
			systemVars.put(key, value);
		}

		Pattern pattern = SimpleFileTemplateImpl.PATTERN;
		Map<String, String> defineParams = template.getDefineParams();
		Iterator<String> it = defineParams.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			// ignore keys in systemVars
			if (systemVars.containsKey(key)) {
				continue;
			}
			// ignore keys like ${key}
			if (Util.matches(pattern, key)) {
				continue;
			}
			String value = defineParams.get(key);
			// replace value
			if (Util.matches(pattern, value)) {
				value = Util.replaceAllParams(systemVars, value, pattern);
			}

			params.put(key, value);
		}

		// add all systemVars to params
		it = systemVars.keySet().iterator();
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
			return externalTemplates.get(k);
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

		combo.add("--choose template--");

		Iterator<String> it = templates.keySet().iterator();
		while (it.hasNext()) {
			combo.add(it.next());
		}

		it = externalTemplates.keySet().iterator();
		while (it.hasNext()) {
			combo.add(it.next());
		}

		if (!templates.isEmpty() || externalTemplates.isEmpty()) {
			combo.select(1);
		} else {
			combo.select(0);
		}
	}

	private void loadExternalTemplates() {
		externalTemplates.clear();
		refreshCombo();
		String fileName = externalTemplatesText.getText();
		File file = new File(fileName);
		if (file.exists()) {
			try {
				loadTemplates(externalTemplates, new FileInputStream(file), "external");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				updateStatus("Load template file : " + fileName + " failed", true);
			}
		} else {
			updateStatus("Template file : " + fileName + " not found", true);
		}
	}

	private void loadInputParams() {
		Template template = getTemplate();
		if (template == null) {
			updateStatus("Selected tempalte not exists");
			return;
		}
		updateStatus(null);
		List<InputParam> inputParams = template.getInputParams();
		if (inputParams == null) {
			return;
		}
		for (InputParamPair ipp : inputParamPairs) {
			ipp.dispose();
		}
		inputParamPairs.clear();

		for (InputParam inputParam : inputParams) {
			final InputParamPair ipp = new InputParamPair(container, inputParam);
			inputParamPairs.add(ipp);
			ipp.value.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent arg0) {
					dialogChanged();
				}
			});
		}

		dialogChanged();
	}
}