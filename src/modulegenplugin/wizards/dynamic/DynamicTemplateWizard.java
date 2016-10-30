package modulegenplugin.wizards.dynamic;

import modulegenplugin.component.TidySelection;
import modulegenplugin.wizards.MyNewWizard;

public class DynamicTemplateWizard extends MyNewWizard {
	public DynamicTemplateWizard() {
		super();
		this.setWindowTitle("Module Generator");
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new DynamicTemplatePage(new TidySelection(selection));
		addPage(page);
	}
}