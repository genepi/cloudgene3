package cloudgene.mapred.wdl;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WdlParameterOutputType {
	LOCAL_FOLDER("local_folder"), LOCAL_FILE("local_file"), WEBPAGE("webpage");

	private String value;

	WdlParameterOutputType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static WdlParameterOutputType getEnum(String value) {
		for (WdlParameterOutputType v : values())
			if (v.getValue().equalsIgnoreCase(value.replaceAll("-", "_")))
				return v;
		return LOCAL_FOLDER;
	}
}
