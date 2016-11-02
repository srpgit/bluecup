package modulegenplugin.component;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Encapsulation of eclipse plugin selection.Make it tidy.
 * 
 * @author RP_S
 * @since 2016/10/30
 *
 */
public class TidySelection {
	private ISelection selection;
	private String projectName;
	private String projectPath;
	private String packageName;
	private String nodePath;
	private String folder;

	public TidySelection(ISelection selection) {
		this.selection = selection;
		init();
	}

	private void init() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();

			projectPath = Platform.getLocation().toString();

			if (obj instanceof IPackageFragment) {
				IPackageFragment ipf = (IPackageFragment) obj;
				nodePath = ipf.getPath().makeAbsolute().toString();
				folder = nodePath;
				packageName = ipf.getElementName();
				projectName = ipf.getJavaProject().getElementName();
			} else if (obj instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot ipf = (IPackageFragmentRoot) obj;
				nodePath = ipf.getPath().makeAbsolute().toString();
				folder = nodePath;
				projectName = ipf.getJavaProject().getElementName();
				packageName = "";
			} else if (obj instanceof IResource) {
				IResource ir = (IResource) obj;
				projectName = ir.getProject().getName();
				nodePath = ir.getFullPath().toString();
				if (ir instanceof IFolder) {
					folder = nodePath;
				} else if (ir instanceof IProject) {
					IProject ip = (IProject) ir;
					folder = ip.getFullPath().toString();
				} else {
					folder = ir.getParent().getFullPath().toString();
				}
			}
		}
	}

	public String getProjectName() {
		return this.projectName;
	}

	public String getProjectPath() {
		return this.projectPath;
	}

	public String getPackageName() {
		return this.packageName;
	}

	public String getNodePath() {
		return this.nodePath;
	}

	public String getFolder() {
		return this.folder;
	}
}
