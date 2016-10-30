package modulegenplugin.wizards;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jface.wizard.IWizardPage;

public interface IModulePage extends IWizardPage {
	Map<String, String> getParams() throws IOException;

	File getTempalteFile();

	String getOutAbsolutePath();

	String getRefreshContainer();

	Template getTemplate();
}
