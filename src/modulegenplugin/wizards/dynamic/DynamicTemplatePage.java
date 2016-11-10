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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.primesoft.sfti.SimpleFileTemplateImpl;
import com.primesoft.sfti.Util;

import modulegenplugin.component.TidySelection;
import modulegenplugin.wizards.MyWizardPage;
import modulegenplugin.wizards.dynamic.template.InputParam;
import modulegenplugin.wizards.dynamic.template.Template;

public class DynamicTemplatePage extends MyWizardPage {
	private Composite container;
	private Text outputPath;

	private final String templatesFile = "templates.zip";

	private Text externalTemplatesText;
	private Map<String, Template> externalTemplates;
	private Button useExternalTemplates;

	private Text externalFolderText;
	private Map<String, Template> externalFolder;
	private Button useExternalFolder;

	private Combo combo;
	private Map<String, Template> templates;

	private Map<String, String> systemVars;
	private List<InputParamPair> inputParamPairs;

	private static ImageDescriptor img = ImageDescriptor.createFromURL(Thread.currentThread().getContextClassLoader().getResource("icon.png"));

	public DynamicTemplatePage(TidySelection selection) {
		super("");
		setTitle("Dynamic Template Module Generator");
		setDescription("Generate a new module or file system by the selected template ");
		setImageDescriptor(img);

		this.templates = new HashMap<String, Template>();
		this.externalTemplates = new HashMap<String, Template>();
		this.externalFolder = new HashMap<String, Template>();
		this.inputParamPairs = new ArrayList<InputParamPair>();
		this.selection = selection;

		systemVars = new HashMap<String, String>();
		systemVars.put("project_path", this.selection.getProjectPath());
		systemVars.put("project_name", this.selection.getProjectName());
		String packageName = this.selection.getPackageName();
		String packageNameAutoDot = this.selection.getPackageName();
		if (packageName == null || packageName.length() == 0) {
			packageNameAutoDot = packageName;
		} else {
			packageNameAutoDot = packageName + ".";
		}
		systemVars.put("package_name", packageName);
		systemVars.put("package_name_autodot", packageNameAutoDot);
		systemVars.put("node_path", this.selection.getNodePath());
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		// choose output destination
		Label label = new Label(container, SWT.NULL);
		label.setText("&Destination Folder:");

		outputPath = new Text(container, SWT.BORDER | SWT.SINGLE);
		outputPath.setEditable(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 400;
		outputPath.setLayoutData(gd);
		outputPath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		final DirectoryDialog outputFolderDialog = new DirectoryDialog(container.getShell(), SWT.OPEN);
		Button chooseOutpath = new Button(container, SWT.PUSH);
		chooseOutpath.setText("Browse...");
		chooseOutpath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String folder = outputFolderDialog.open();
				if (folder != null) {
					outputPath.setText(folder);
				}
			}
		});

		// external folder
		useExternalFolder = new Button(container, SWT.CHECK);
		useExternalFolder.setText("&Open Folder:");

		externalFolderText = new Text(container, SWT.BORDER | SWT.SINGLE);
		externalFolderText.setEditable(false);
		externalFolderText.setEnabled(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 400;
		externalFolderText.setLayoutData(gd);
		externalFolderText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		final DirectoryDialog folderDialog = new DirectoryDialog(container.getShell(), SWT.OPEN);

		final Button extFolderButton = new Button(container, SWT.PUSH);
		extFolderButton.setText("Browse...");
		extFolderButton.setEnabled(false);
		extFolderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fileName = folderDialog.open();
				if (fileName != null) {
					externalFolderText.setText(fileName);
					loadExternalFolder();
				}
			}
		});

		useExternalFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (useExternalFolder.getSelection()) {
					externalFolderText.setEnabled(true);
					extFolderButton.setEnabled(true);
					if (externalFolderText.getText().length() > 0) {
						loadExternalFolder();
					}
				} else {
					externalFolderText.setEnabled(false);
					extFolderButton.setEnabled(false);
					externalFolder.clear();
					refreshCombo();
					loadInputParams();
				}
			}
		});

		// external zip file
		useExternalTemplates = new Button(container, SWT.CHECK);
		useExternalTemplates.setText("&Open Zip:");

		externalTemplatesText = new Text(container, SWT.BORDER | SWT.SINGLE);
		externalTemplatesText.setEditable(false);
		externalTemplatesText.setEnabled(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 400;
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

		useExternalTemplates.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (useExternalTemplates.getSelection()) {
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
					loadInputParams();
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Template:");

		combo = new Combo(container, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		gd.minimumWidth = 400;
		combo.setLayoutData(gd);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadInputParams();
			}
		});

		// empty
		label = new Label(container, SWT.NULL);

		String folder = this.selection.getFolder();
		if (folder != null) {
			outputPath.setText(folder);
		}

		loadTemplates(this.templates, Thread.currentThread().getContextClassLoader().getResourceAsStream(templatesFile), "default");
		loadInputParams();

		dialogChanged();

		container.layout();
		setControl(container);
	}

	private void dialogChanged() {
		String outputPath = getOutputPath();
		if (outputPath.length() == 0) {
			updateStatus("Unknown destination folder");
			return;
		}
		if (!new File(outputPath).exists()) {
			IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(outputPath));
			if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
				updateStatus("Destination folder must exist");
				return;
			}
			if (!container.isAccessible()) {
				updateStatus("Project must be writable");
				return;
			}
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
			if (ipp.pattern != null) {
				if (!Util.matches(ipp.pattern, ipp.value.getText())) {
					updateStatus(ipp.inputParam.getTip(), false);
					return;
				}
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

	public String getOutputPath() {
		return outputPath.getText();
	}

	@Override
	public Map<String, String> getParams() throws IOException {
		Template template = getTemplate();
		if (template == null) {
			throw new RuntimeException("No tempalte is selected");
		}
		Pattern pattern = SimpleFileTemplateImpl.PATTERN;

		// result params
		Map<String, String> params = new HashMap<String, String>();

		// add input params
		for (int i = 0; i < inputParamPairs.size(); i++) {
			InputParamPair ipp = inputParamPairs.get(i);
			String key = ipp.name.getText();
			if (key == null) {
				continue;
			}
			// remove last ':'
			key = key.substring(0, key.length() - 1);
			String value = ipp.value.getText();
			// replace value
			if (Util.matches(pattern, value)) {
				value = Util.replaceAllParams(systemVars, value, pattern);
			}
			// can override system vars
			systemVars.put(key, value);
		}

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

		// ensure no value like '${xxx}'
		it = params.keySet().iterator();
		while (it.hasNext()) {
			String k = it.next();
			String v = params.get(k);
			if (Util.matches(pattern, v)) {
				it.remove();
			}
		}

		return params;
	}

	@Override
	public Template getTemplate() {
		String k = combo.getText();
		if (k.startsWith("[default]")) {
			return templates.get(k);
		} else if (k.startsWith("[zip]")) {
			return externalTemplates.get(k);
		} else if (k.startsWith("[folder]")) {
			return externalFolder.get(k);
		}
		return null;
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
		String outputPath = getOutputPath();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(outputPath));
		if (resource != null) {
			return resource.getLocationURI().getPath();
		} else {
			return outputPath;
		}
	}

	@Override
	public String getRefreshContainer() {
		return getOutputPath();
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

		loadFolder(map, tf, namespace);
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

		it = externalFolder.keySet().iterator();
		while (it.hasNext()) {
			combo.add(it.next());
		}

		if (!(templates.isEmpty() && externalTemplates.isEmpty() && externalFolder.isEmpty())) {
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
				loadTemplates(externalTemplates, new FileInputStream(file), "zip");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				updateStatus("Load template file[" + fileName + "]failed", true);
			}
		} else {
			updateStatus("Template file[" + fileName + "]not found,can not load", true);
		}
	}

	private void loadInputParams() {
		for (InputParamPair ipp : inputParamPairs) {
			ipp.dispose();
		}

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

	private void loadExternalFolder() {
		externalFolder.clear();
		refreshCombo();
		String fileName = externalFolderText.getText();
		File file = new File(fileName);
		if (file.exists()) {
			try {
				loadFolder(externalFolder, file, "folder");
			} catch (Exception e) {
				e.printStackTrace();
				updateStatus("Load folder[" + fileName + "]failed", true);
			}
		} else {
			updateStatus("Folder[" + fileName + "]not found,can not load", true);
		}
	}

	private void loadFolder(Map<String, Template> map, File tf, String namespace) {
		// 将templates下的每个子级文件夹视为一个模块
		File[] files = tf.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		if (files != null && files.length > 0) {
			for (File file : files) {
				map.put("[" + namespace + "]->" + file.getName(), new Template(file.getAbsolutePath()));
			}
		}
		refreshCombo();
	}
}