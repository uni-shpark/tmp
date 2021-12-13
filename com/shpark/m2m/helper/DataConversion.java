package com.shpark.m2m.helper;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DataConversion {

	static String loc_code = "";
	
	public static JSONArray tableAnalyze(JSONObject jobj) {
	
		JSONArray result = new JSONArray();
		
		Set<String> set = jobj.keySet();
		Iterator<String> it = set.iterator();
		
		while (it.hasNext()) {
			
			JSONObject row = new JSONObject();
			String partitions = it.next();
			String tables = ""+jobj.get(partitions);
			
			if("".equals(tables))
				continue;
			
			row.put("partition", partitions);
			row.put("tables", tables.toUpperCase());
			
			result.add(row);
			
		}
		
		return result;
	}
	
	
	public static JSONArray partitionsconversion(List<String> partitions) {
		
		JSONArray result = new JSONArray();
		
		for (String partition : partitions) {
			JSONObject obj = new JSONObject();
			obj.put("name",partition);
			result.add(obj);
		}
		return result;
	}
	
	
	public static JSONArray contentsConversion(JSONArray contents) {
		
		JSONArray result = new JSONArray();
		for (int i = 0; i < contents.size(); i++) {
			JSONObject content = new JSONObject();
			content.put("name", ""+contents.get(i));
			result.add(content);
		}
		
		return result;
	}
	
	private static int getCalledCnt(JSONObject methods) {

		Set<String> set = methods.keySet();
		Iterator<String> it = set.iterator();

		int cnt = 0;
		while (it.hasNext()) {

			String method = it.next();
			cnt = cnt + Integer.parseInt("" + methods.get(method));

		}
		return cnt;

	}

	public static boolean compare(JSONObject source, JSONArray sources) {

		if (sources == null)
			return false;

		String target_str = "" + source.get("target");
		
		for (int i = 0; i < sources.size(); i++) {

			JSONObject source_2 = (JSONObject) sources.get(i);
			String source_str = "" + source_2.get("source_simple");

			if (source_str.equals(target_str))
				return true;

		}
		return false;
	}

	public static JSONObject cardinalMerge(JSONObject common_data, JSONObject links, JSONArray result) {

		JSONObject merged = new JSONObject();

		Set<String> set = links.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String target_nm = it.next();
			JSONArray sources = (JSONArray) links.get(target_nm);
			JSONObject target_data = (JSONObject) common_data.get(target_nm);

			if(target_data == null) {
				System.out.println(sources.toJSONString());
				continue;
			}
			String class_full_nm = "" + target_data.get("class_full_nm");
			String category = "" + target_data.get("category");

			JSONObject merged_sub = new JSONObject();

			merged_sub.put("category", category);
			merged_sub.put("name", target_nm);

			for (int i = 0; i < sources.size(); i++) {
				JSONArray source_partitions = new JSONArray();
				JSONObject link = (JSONObject) sources.get(i);

				String source = "" + link.get("source");
				if(class_full_nm.endsWith("ProceedAdvice.java")) {
				}
				if (sources.size() < 2 && compare(link, (JSONArray) links.get(link.get("source_simple"))))
					continue;

				String source_category = "" + link.get("source_partition");
				if (source_category.equals(category))
					continue;

				int called = getCalledCnt((JSONObject) link.get("target_methods"));

				JSONObject source_data = new JSONObject();
				source_data.put("source", source);
				source_data.put("source_category", source_category);
				source_data.put("called", called);
				source_data.put("target_category", category);

				if (merged.containsKey(class_full_nm)) {
					JSONObject sub = (JSONObject) merged.get(class_full_nm);
					source_partitions = (JSONArray) sub.get("links");
				}
				source_partitions.add(source_data);
				merged_sub.put("links", source_partitions);
				merged.put(class_full_nm, merged_sub);
			}
		}

		JSONArray errors = new JSONArray();
		if (result == null)
			return merged;

		for (int i = 0; i < result.size(); i++) {
			JSONObject error_row = (JSONObject) result.get(i);
			if (error_row.containsKey("error")) {
				errors.add(error_row.get("error"));
			}
		}
		if (errors.size() > 0) {
			merged.put("errors", errors);
		}
		return merged;
	}

	public static JSONObject inheritanceMerge(JSONObject inheritance, JSONObject history, JSONArray result) {

		Set<String> set = history.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String key = it.next();
			JSONArray children_history = (JSONArray) history.get(key);
			JSONArray children_inheritance = null;

			String key_for_inheritance = key.split("@")[0];
			if (inheritance.containsKey(key_for_inheritance)) {
				children_inheritance = (JSONArray) inheritance.get(key_for_inheritance);
			}

			if (children_inheritance != null) {

				for (int i = 0; i < children_history.size(); i++) {

					String history_child = "" + children_history.get(i);

					String history_child_key = history_child.split("@")[0];
					if (children_inheritance.contains(history_child_key)) {
						children_inheritance.remove(history_child_key);
					}
					children_inheritance.add(children_history.get(i));
				}
			}

			inheritance.put(key, children_inheritance);

		}

		JSONArray errors = new JSONArray();
		if (result == null)
			return inheritance;

		for (int i = 0; i < result.size(); i++) {
			JSONObject error_row = (JSONObject) result.get(i);
			if (error_row.containsKey("error")) {
				errors.add(error_row.get("error"));
			}
		}
		if (errors.size() > 0) {
			inheritance.put("errors", errors);
		}
		return inheritance;

	}

	public static JSONArray cardinalConversion(JSONObject result, JSONObject history_data) {

		JSONArray output = new JSONArray();

		Set<String> set = result.keySet();
		Iterator<String> it = set.iterator();

//		System.out.println(result.toJSONString());
		while (it.hasNext()) {

			JSONArray output_sources = new JSONArray();

			String class_full_nm = it.next();
			JSONObject label = new JSONObject();
			
			if ("errors".equals(class_full_nm)) {
				label.put("errors", class_full_nm);
				label.put("msg", ((JSONArray) (result.get(class_full_nm))).toJSONString());
				output.add(label);
				continue;
			}
			
			JSONObject class_data = (JSONObject) result.get(class_full_nm);

			String name = "" + class_data.get("name");
			String category = "" + class_data.get("category");
			JSONArray links = (JSONArray) class_data.get("links");

			String class_full_nm_tmp = class_full_nm;
			if (class_full_nm_tmp.length() > 90) {
				class_full_nm_tmp = class_full_nm_tmp.substring(0, 90) + ".....";
			}

			String history_check_key = class_full_nm.replace("/", ".").replace(".java", "");
			
			if(history_data.containsKey(history_check_key)) {
				label.put("category", "commons");
			} else {
				label.put("category", category);
			}
			label.put("label", class_full_nm_tmp.replace("/", ".").replace(".java", ""));
			label.put("label_tmp", class_full_nm.replace("/", ".").replace(".java", ""));
			label.put("org_category", category);
			label.put("html", false);

			for (int i = 0; i < links.size(); i++) {

				boolean isCopied = false;
				JSONObject row = new JSONObject();
				JSONObject link = (JSONObject) links.get(i);

				String source = "" + link.get("source");
				String source_tmp = source;
				String source_category = "" + link.get("source_category");
				String target_category = "" + link.get("target_category");

				int called = (int) link.get("called");

				history_check_key = source_tmp.replace("/", ".").replace(".java", "");
				if (history_data.containsKey(history_check_key)) {

					String  copied_partition = (""+ history_data.get(history_check_key)).split("&&")[1];
					if (copied_partition.equals(source_category) || "Unobserved".equals(source_category))
						isCopied = true;
				}
				if(isCopied)
					row.put("source_partition", "commons");
				else
					row.put("source_partition", source_category);
				
				isCopied = false;
				row.put("org_source_partition", source_category);
				row.put("source", source_tmp.replace("/", ".").replace(".java", ""));
				row.put("source_tmp", source.replace("/", ".").replace(".java", ""));
				row.put("called", called);
				row.put("target", class_full_nm_tmp.replace("/", ".").replace(".java", ""));
				row.put("target_tmp", class_full_nm.replace("/", ".").replace(".java", ""));
				
				history_check_key = class_full_nm.replace("/", ".").replace(".java", "");
				if (history_data.containsKey(history_check_key)) {

					String  copied_partition = ""+ history_data.get(history_check_key);
					if (copied_partition.indexOf(target_category) > 0)
						isCopied = true;
				}
				if(isCopied)
					row.put("target_partition", "commons");
				else
					row.put("target_partition", target_category);

				output_sources.add(row);
//				output.add(row);

			}
			label.put("children", output_sources);
			output.add(label);
		}

		return output;
	}

	
	private static boolean isExists(String full_nm) {

		String modify = full_nm.replace(".", "/") + ".java";
		File loc_code = new File(DataConversion.loc_code + "/" + modify);

		return loc_code.exists();

	}
	
	public static JSONArray inheritanceConversion(JSONObject result, String loc_code) {
		
		DataConversion.loc_code = loc_code;
		return inheritanceConversion(result);
		
	}
	
	public static JSONArray inheritanceConversion(JSONObject result) {

		JSONArray output = new JSONArray();

		Set<String> set = result.keySet();
		Iterator<String> it = set.iterator();

		System.out.println(result.toJSONString());
		while (it.hasNext()) {

			String parent = it.next();
			String parent_tmp = parent;
			if (parent_tmp.length() > 60) {
				parent_tmp = parent_tmp.substring(0, 60) + ".....";
			}

			if(!isExists(parent))
				continue;
			
			JSONArray children = (JSONArray) result.get(parent);
			JSONArray output_children = new JSONArray();

			JSONObject label = new JSONObject();

			if ("errors".equals(parent)) {
				label.put("errors", parent_tmp);
				label.put("msg", ((JSONArray) (result.get(parent))).toJSONString());
				output.add(label);
				continue;
			}
			label.put("label", parent_tmp);
			label.put("label_tmp", parent);
			label.put("html", false);
//			label.put("mode", "span");

			if(children == null) {
				return output;
			}
			for (int i = 0; i < children.size(); i++) {

				JSONObject row = new JSONObject();

				String[] child = ("" + children.get(i)).split("@");
				int size = child.length;

				switch (size) {

				case 1:
					row.put("parent", parent_tmp);
					row.put("parent_tmp", parent);
					row.put("parent_org", parent);
					break;
				case 2:

					String new_parent = child[1];
					String new_parent_tmp = new_parent;

					if (new_parent_tmp.length() > 60) {
						new_parent_tmp = new_parent_tmp.substring(0, 60) + ".....";
					}

					row.put("parent", new_parent_tmp);
					row.put("parent_tmp", new_parent);
					row.put("parent_org", parent);
				}

				String child_tmp = child[0];
				if (child_tmp.length() > 60) {
					child_tmp = child_tmp.substring(0, 60) + ".....";
				}

				row.put("child", child_tmp);
				row.put("child_tmp", child[0]);

				output_children.add(row);
//				output.add(row);

			}
			label.put("children", output_children);
			output.add(label);
		}

		return output;
	}

}
