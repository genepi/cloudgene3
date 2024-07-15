package cloudgene.mapred.jobs;

import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.steps.BashCommandStep;
import cloudgene.mapred.steps.GroovyStep;
import cloudgene.mapred.steps.JavaExternalStep;
import cloudgene.mapred.wdl.WdlStep;

public class CloudgeneStepFactory {

	private static CloudgeneStepFactory instance = null;
	
	private Map<String, Class> registeredClasses;

	public static CloudgeneStepFactory getInstance() {
		if (instance == null) {
			instance = new CloudgeneStepFactory();
		}
		return instance;
	}
	
	private CloudgeneStepFactory() {
		registeredClasses = new HashMap<String, Class>();
		register("java", JavaExternalStep.class);
		register("groovy", GroovyStep.class);
		register("command", BashCommandStep.class);
	}
	
	public void register(String type, Class clazz) {
		registeredClasses.put(type, clazz);
	}
	
	public Class getClassname(WdlStep step) {

		if (step.getClassname() != null) {
			try {
				return Class.forName(step.getClassname());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Class '" + step.getClassname() + "' not found.");
			}
		}

		String type = step.getString("type");
		if (type == null) {
			type = "nextflow";
		}

		Class clazz = registeredClasses.get(type);
		if (clazz != null) {
			return clazz;
		}

		throw new RuntimeException("Unknown type: '" + type + "'");

	}

}
