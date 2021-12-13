package com.shpark.m2m.helper;

import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RefView {

	public static JSONArray load(JSONArray target, String source) {

		JSONArray result = new JSONArray();
		JSONObject label = new JSONObject();

		for (int i = 0; i < target.size(); i++) {
			JSONObject source_data = (JSONObject) target.get(i);

			String source_nm = "" + source_data.get("source_simple");
			String target_nm = "" + source_data.get("target");
			if (source_nm.equals(source)) {

				label.put("label", target_nm);
				label.put("source", source_nm);
				label.put("children", getMethods((JSONObject) source_data.get("target_methods")));

				result.add(label);
				return result;
			}
		}
		return null;
	}

	private static JSONArray getMethods(JSONObject target_methods) {

		JSONArray methods = new JSONArray();
		Set<String> set = target_methods.keySet();

		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String method_nm = it.next();
			JSONObject method = new JSONObject();
			method.put("method_nm", method_nm);
			method.put("called", target_methods.get(method_nm));

			methods.add(method);

		}

		return methods;

	}
}
