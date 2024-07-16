package cloudgene.mapred.apps;

import java.io.IOException;

import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlReader;
import genepi.io.FileUtil;

public class Application implements Comparable<Application> {

	private String filename;

	private String permission;

	private boolean syntaxError = false;

	private WdlApp wdlApp = null;

	private String errorMessage = "";

	private boolean enabled = true;

	public Application() {

	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getPermission() {
		return permission;
	}

	public String[] getPermissions() {
		return permission.split(",");
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getId() {
		return wdlApp.getId() + "@" + wdlApp.getVersion();
	}

	public void loadWdlApp() throws IOException {
		try {
			wdlApp = WdlReader.loadAppFromFile(getFilename());
			syntaxError = false;
			errorMessage = "";
		} catch (IOException e) {
			syntaxError = true;
			wdlApp = null;
			errorMessage = e.getMessage();
			throw e;
		}
	}

	public WdlApp getWdlApp() {
		return wdlApp;
	}

	public boolean hasSyntaxError() {
		return syntaxError;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isLoaded() {
		return wdlApp != null;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getType() {
		if (isLoaded()) {
			if (wdlApp.getWorkflow() != null) {
				return "Application";
			} else {
				if (wdlApp.getCategory() != null && !wdlApp.getCategory().isEmpty()) {
					return wdlApp.getCategory();
				} else {
					return "Package";
				}
			}
		} else {
			return "-";
		}
	}

	@Override
	public int compareTo(Application o) {
		// sort by type
		int result = getType().compareTo(o.getType());
		if (result != 0) {
			return result;
		}
		// sort by name
		result =  wdlApp.getName().compareTo(o.wdlApp.getName());
		if (result != 0) {
			return result;
		}
		// sort by version
		return  wdlApp.getVersion().compareTo(o.wdlApp.getVersion());
	}

}
