package cn.telcom.enrol.config.autoproducecode;

import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;

import static org.mybatis.generator.internal.util.messages.Messages.getString;

public class ShellCallbackEx extends DefaultShellCallback {

	public ShellCallbackEx(boolean overwrite) {
		super(overwrite);
	}

	@Override
	public File getDirectory(String targetProject, String targetPackage) throws ShellException {

		File project = new File(targetProject);
		if (!project.isDirectory()) {
			throw new ShellException(getString("Warning.9", //$NON-NLS-1$
					targetProject));
		}
		return project;
	}

}
