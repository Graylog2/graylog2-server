package org.graylog2.database;

import org.bson.types.ObjectId;

public interface Persistable {

	public ObjectId getId();
	
}
