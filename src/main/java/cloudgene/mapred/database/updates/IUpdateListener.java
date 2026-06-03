package cloudgene.mapred.database.updates;

import cloudgene.mapred.database.util.Database;

public interface IUpdateListener {

	public void beforeUpdate(Database database);
	
	public void afterUpdate(Database database);
	
}
