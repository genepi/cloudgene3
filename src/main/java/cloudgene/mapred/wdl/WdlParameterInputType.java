package cloudgene.mapred.wdl;

public enum WdlParameterInputType {
	LOCAL_FOLDER("local_folder"),

	LOCAL_FILE("local_file"),

	TEXT("text"),

	STRING("string"),

	CHECKBOX("checkbox"),

	LIST("list"),

	RADIO("radio"),

	NUMBER("number"),

	LABEL("label"),

	INFO("info"),

	AGBCHECKBOX("agbcheckbox"),

	TERMS_CHECKBOX("terms_checkbox"),
	
	GROUP("group"),

	APP_LIST("app_list"),

	SEPARATOR("separator"),

	TEXTAREA("textarea");

	private String value;

	WdlParameterInputType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static WdlParameterInputType getEnum(String value) {
		String cleanValue = value.replaceAll("-", "_");
		for (WdlParameterInputType parameterInputType : values())
			if (parameterInputType.getValue().equalsIgnoreCase(cleanValue))
				return parameterInputType;

		if (cleanValue.equalsIgnoreCase("file")) {
			return LOCAL_FILE;
		}

		if (cleanValue.equalsIgnoreCase("folder")) {
			return LOCAL_FOLDER;
		}

		if (cleanValue.equalsIgnoreCase("dataset")) {
			return APP_LIST;
		}

		throw new IllegalArgumentException("Value '" + value + "' is not a valid type.");
	}
}
