package modulegenplugin.wizards;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the
 * provided container. If the container resource (a folder or a project) is
 * selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "mpe". If a
 * sample multi-page editor (also available as a template) is registered for the
 * same extension, it will be able to open it.
 */

public class DynamicTemplateWizard extends IMyNewWizard {

	/**
	 * Constructor for SampleNewWizard.
	 */
	public DynamicTemplateWizard() {
		super();
		this.setWindowTitle("Module Generator");
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new DynamicTemplatePage(selection);
		addPage(page);
	}
}