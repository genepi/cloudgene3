package cloudgene.mapred.wdl;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;
import java.util.Vector;

@JsonClassDescription
public class WdlParameterOutput implements WdlParameter {

	private String id;

	private String description;

	//needed, because yamlbeans expects property AND getter/setter methods.
	private String type;
	
	private WdlParameterOutputType typeEnum;

	private boolean download = true;

	private boolean autoExport = false;

	private boolean adminOnly = false;

	private boolean serialize = true;

	private List<String> includes = new Vector<String>();

	private List<String> excludes = new Vector<String>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Deprecated
	public String getType() {
		return typeEnum.toString();
	}

	public void setType(String type) {
		this.typeEnum = WdlParameterOutputType.getEnum(type);
	}

	public WdlParameterOutputType getTypeAsEnum() {
		return typeEnum;
	}

	public boolean isDownload() {
		return download;
	}

	public void setDownload(boolean download) {
		this.download = download;
	}

	public void setAutoExport(boolean autoExport) {
		this.autoExport = autoExport;
	}

	public boolean isAutoExport() {
		return autoExport;
	}

	public void setAdminOnly(boolean adminOnly) {
		this.adminOnly = adminOnly;
	}

	public boolean isAdminOnly() {
		return adminOnly;
	}

	public boolean isSerialize() {
		return serialize;
	}

	public void setSerialize(boolean serialize) {
		this.serialize = serialize;
	}

	public boolean isFileOrFolder() {
		return (typeEnum == WdlParameterOutputType.LOCAL_FILE || typeEnum == WdlParameterOutputType.LOCAL_FOLDER || typeEnum == WdlParameterOutputType.WEBPAGE);
	}

	public boolean isFolder() {
		return (typeEnum == WdlParameterOutputType.LOCAL_FOLDER || typeEnum == WdlParameterOutputType.WEBPAGE);
	}

	public boolean isWebpage() {
		return (typeEnum == WdlParameterOutputType.WEBPAGE);
	}

	public List<String> getIncludes() {
		return includes;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}
}
