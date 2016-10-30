package modulegenplugin.wizards;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;

import modulegenplugin.component.TidySelection;
import modulegenplugin.wizards.dynamic.template.Template;

public abstract class MyWizardPage extends WizardPage {
	protected TidySelection selection;

	protected MyWizardPage(String pageName) {
		super(pageName);
	}

	protected MyWizardPage(TidySelection selection) {
		super("");
		this.selection = selection;
	}

	public abstract Map<String, String> getParams() throws IOException;

	public abstract File getTempalteFile();

	public abstract String getOutAbsolutePath();

	public abstract String getRefreshContainer();

	public abstract Template getTemplate();
}
