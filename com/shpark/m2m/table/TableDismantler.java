package com.shpark.m2m.table;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Resource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import com.shpark.m2m.bci.server.helper.DataHelper;
import com.shpark.m2m.helper.Commons;

@Component
public class TableDismantler {

	JSONArray cardinal;
	JSONArray rollback;
	JSONArray clean_classes;
	JSONObject modifying;
	String loc_msa_code;
	String loc_logs;
	String loc_org_src;
	Properties history_props;

	@Resource
	Commons commons;

	/**
	 * 
	 * @param inheritance 부모 / [자식] - JSONObject, JSONArray
	 * @param modifying   자식 / 변경할 부모 클래스명 - JSONObject, JSONObject
	 */

	public TableDismantler() {

	}

	public JSONObject tableAnalyze(JSONArray data) {

		List<String> partition_list = commons.getPartitions();
//		JSONObject combine_data = getPartitionCombine(partition_list);
		JSONObject combine_data = new JSONObject();

		List<String> table_list = DataHelper.getTables();

		for (String table_key : table_list) {

			TreeMap<String, String> tableMap = new TreeMap<String, String>();

			for (int i = 0; i < data.size(); i++) {

				JSONObject obj = (JSONObject) data.get(i);

				String partition = "" + obj.get("partition");

				if ("".equals(partition)) {
					System.out.println();
				}
				String tables = ("" + obj.get("tables")).toLowerCase();

				if (tables.indexOf(table_key) >= 0) {
					tableMap.put(partition, "");
				} else
					continue;
			}

			String partitions = merged(tableMap);
			if ("".equals(partitions)) {
				continue;
			}
			if (combine_data.containsKey(partitions)) {
				String merged_table = ("" + combine_data.get(partitions)).trim();

				if (merged_table.length() == 0) {
					combine_data.put(partitions, table_key);
				} else {
					merged_table += ", " + table_key;
					combine_data.put(partitions, merged_table);
				}

			} else {
				combine_data.put(partitions, table_key);
			}
		}

		return combine_data;

	}

	private String merged(TreeMap<String, String> tableMap) {

		StringBuffer partitions = new StringBuffer();

		Set<String> set = tableMap.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			if (partitions.toString().length() == 0) {
				partitions.append(it.next());
			} else {
				partitions.append(" / " + it.next());
			}
		}
		return partitions.toString();
	}

//	public JSONObject getPartitionCombine(List<String> arr) {
//		int n = arr.size();
//		boolean[] visited = new boolean[n];
//
//		JSONObject combine = new JSONObject();
//		for (int i = 1; i <= n; i++) {
//			comb(arr, visited, 0, n, i, combine);
//		}
////		System.out.println(combine.toString());
//
//		return combine;
//	}

//	static void comb(List<String> arr, boolean[] visited, int depth, int n, int r, JSONObject combine) {
//		if (r == 0) {
//			print(arr, visited, n, combine);
//			return;
//		}
//
//		if (depth == n) {
//			return;
//		}
//
//		visited[depth] = true;
//		comb(arr, visited, depth + 1, n, r - 1, combine);
//
//		visited[depth] = false;
//		comb(arr, visited, depth + 1, n, r, combine);
//	}
//
//	static void print(List<String> arr, boolean[] visited, int n, JSONObject combine) {
//
//		String value = "";
//		for (int i = 0; i < n; i++) {
//			if (visited[i]) {
//				System.out.print(arr.get(i) + " ");
//
//				if (!"".equals(value))
//					value += " / " + arr.get(i);
//				else
//					value += arr.get(i);
//			}
//		}
//
//		if (!"".equals(value)) {
//			combine.put(value, "");
//		}
//	}

}
