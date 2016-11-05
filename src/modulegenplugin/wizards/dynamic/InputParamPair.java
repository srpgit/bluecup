package modulegenplugin.wizards.dynamic;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import modulegenplugin.wizards.dynamic.template.InputParam;

public class InputParamPair {
	InputParam inputParam;
	Pattern pattern;

	Label name;
	Text value;
	Label empty;

	public InputParamPair(Composite parent, InputParam param) {
		this.inputParam = param;
		try {
			String regex = param.getRegex();
			if (regex != null && regex.length() > 0) {
				this.pattern = Pattern.compile(regex);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		name = new Label(parent, SWT.NULL);
		name.setText(param.getName() + ":");
		name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		value = new Text(parent, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 400;
		value.setLayoutData(gd);

		empty = new Label(parent, SWT.NULL);

		parent.layout();
	}

	public void dispose() {
		name.dispose();
		value.dispose();
		empty.dispose();
	}
}
