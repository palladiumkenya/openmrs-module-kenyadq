package org.openmrs.module.kenyadq.api.db;

import java.util.List;
import java.util.Map;

/**
 * Add description of the class
 */
public interface KenyaDqDao {
	public List<Object> executeSqlQuery(String query, Map<String, Object> substitutions);
	public List<Object> executeHqlQuery(String query, Map<String, Object> substitutions);
}
