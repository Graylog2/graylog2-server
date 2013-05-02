package org.graylog2.database;

import org.bson.types.ObjectId;
import org.graylog2.database.validators.Validator;

import java.util.Map;

public interface Persistable {

	public ObjectId getId();

}
