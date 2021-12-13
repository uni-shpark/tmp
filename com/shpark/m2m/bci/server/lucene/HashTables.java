package com.shpark.m2m.bci.server.lucene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;

public class HashTables {

	static Map<String, String> cache = new HashMap<String, String>();
	
	static {
		List<Document> documents = Finder.findAll();
		
		for (Document document : documents) {
			dup(document.get("custom_stacks"),document.get("sqls"));
		}
	}
	
	public static String hello() {
		return "Lucene hello";
	}
	public static synchronized boolean dup(String stack, String sql) {

		String key = stack+sql;
		key = key.replaceAll("\n", "");
		if (cache.containsKey(key)) {
				return false;
		} else {
			cache.put(key, "");
		}
		return true;
	}
}
