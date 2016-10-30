package modulegenplugin.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.primesoft.sfti.SimpleFileTemplateImpl;

public abstract class MyNewWizard extends Wizard implements INewWizard {
	protected MyWizardPage page;
	protected IWorkbench workbench;
	protected ISelection selection;

	@Override
	public void init(IWorkbench paramIWorkbench, IStructuredSelection paramIStructuredSelection) {
		this.workbench = paramIWorkbench;
		this.selection = paramIStructuredSelection;
	}

	@Override
	public boolean performFinish() {
		try {
			SimpleFileTemplateImpl.gen(page.getParams(), page.getTempalteFile(), page.getOutAbsolutePath(), "utf-8",
					false);
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource resource = root.findMember(new Path(page.getRefreshContainer()));
			if (resource != null) {
				resource.refreshLocal(1, null);
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
