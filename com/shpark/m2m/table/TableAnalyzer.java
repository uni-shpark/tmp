package com.shpark.m2m.table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import com.shpark.m2m.bci.server.helper.DataHelper;
import com.shpark.m2m.bci.server.lucene.Finder;
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.helper.DataConversion;
import com.shpark.m2m.helper.history.CardinalHistoryManager;
import com.shpark.m2m.util.Configure;

@Component
public class TableAnalyzer implements Constant {

	String loc_cardinal_graph;
	String loc_code;
	JSONArray merged;
	JSONObject cardinal_history;

	public TableAnalyzer() {
	}

	private JSONObject getCommonData(JSONObject org_data) {

		JSONObject common_data = new JSONObject();

		Set<String> set = org_data.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String key = it.next();
			System.out.println(key);
//			JSONObject tmp = (JSONObject)org_data.get(key);
			if (!key.equals("micro_detail_partition_by_business_logic"))
				continue;

			JSONObject cardinal_modified = (JSONObject) org_data.get(key);

			Object obj = cardinal_modified.get("nodes");

			if (obj == null)
				continue;

			JSONArray nodes = (JSONArray) obj;

			for (int i = 0; i < nodes.size(); i++) {

				JSONObject class_data = (JSONObject) nodes.get(i);
				JSONObject class_data_modified = new JSONObject();

				String class_nm = "" + class_data.get("name");
				String class_full_nm = "" + class_data.get("filepath");

				class_full_nm = class_full_nm.split(",")[1].trim();
				class_full_nm = class_full_nm.replaceAll(org_src_dir_nm+"/", "");
				String category = "" + class_data.get("category");

				class_data_modified.put("class_full_nm", class_full_nm);
				class_data_modified.put("class_nm", class_nm);
				class_data_modified.put("category", category);

				if (common_data.containsKey(class_nm)) {
					common_data.put(class_full_nm, class_data_modified);
				} else {
					common_data.put(class_nm, class_data_modified);
				}

			}
		}

		return common_data;

	}

	private boolean isDao (String class_full_nm) {
		
		if(class_full_nm.endsWith("C0.java") || class_full_nm.endsWith("S0.java") 
				|| class_full_nm.endsWith("S1.java") || class_full_nm.endsWith("C1.java")
				|| class_full_nm.endsWith("S2.java") || class_full_nm.endsWith("C2.java")
				|| class_full_nm.endsWith("S3.java") || class_full_nm.endsWith("C3.java")
				|| class_full_nm.endsWith("S4.java") || class_full_nm.endsWith("C4.java")
				|| class_full_nm.endsWith("P0.java") || class_full_nm.endsWith("P1.java")
				|| class_full_nm.endsWith("P5.java") || class_full_nm.endsWith("P35.java")
				|| class_full_nm.endsWith("P6.java") || class_full_nm.endsWith("CN.java")
				|| class_full_nm.endsWith("SS.java") || class_full_nm.endsWith("CC.java")) {
			return true;
		}
		return false;
	}
	
	private JSONArray getTableView() {

		Map<String, String> daoList = DataHelper.getDaoList();
		JSONObject common_data = null;
		JSONParser parser = new JSONParser();

		try {
			File loc_symtable = new File(this.loc_cardinal_graph);

			BufferedReader br = new BufferedReader(new FileReader(loc_symtable));

			Object obj = parser.parse(br);

			if (!(obj instanceof JSONObject)) {
				return null;
			}

			common_data = getCommonData((JSONObject) obj);
			{
				Set<String> set = common_data.keySet();
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {

					String class_nm = it.next();
					JSONObject common_class_data = (JSONObject) common_data.get(class_nm);
					String class_full_nm = "" + common_class_data.get("class_full_nm");
					
					if (!(class_nm.endsWith("Dao") || class_nm.endsWith("DAO"))) {
						
						String accept = "" + daoList.get("accept_dao_class");
						if (accept.indexOf(class_nm) < 0) {

							String package_nm = class_full_nm.substring(0, class_full_nm.lastIndexOf("/"));

							if (package_nm.endsWith("unit") && isDao(class_full_nm) ) {
								System.out.println(" expection class : " + class_full_nm);
							}
							else {
								continue;
							}
						}
					}

					String ignore = "" + daoList.get("ignore_dao_class");

					if (class_nm.indexOf("Klu") == 0) {
						continue;
					}
					if (ignore.indexOf(class_nm) >= 0) {
						continue;
					}

					class_full_nm = class_full_nm.replaceAll("/", ".").substring(0, class_full_nm.lastIndexOf("."));

					String partition = "" + common_class_data.get("category");

					if (this.cardinal_history != null && this.cardinal_history.containsKey(class_full_nm)) {
						partition = "commons";
					}

					if("Unobserved".equals(partition)) {
						continue;
					}
					Finder.findSoftlyWithJArraySimple(class_full_nm, partition, this.merged);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return this.merged;
	}

	public JSONArray getTableList(String loc_cardinal_graph, JSONArray merged, JSONObject cardinal_history) {
		this.loc_cardinal_graph = loc_cardinal_graph;
		this.merged = merged;
		this.cardinal_history = cardinal_history;

		return getTableView();
	}
}
