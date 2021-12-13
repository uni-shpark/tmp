package com.shpark.m2m.cardinal;

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
public class CardinalAnalyzer implements Constant {

	String loc_final_graph;
	String loc_org_final_graph;
	
	String loc_code;
	JSONArray merged;
	JSONObject cardinal_history;
	public CardinalAnalyzer() {
	}

	private JSONObject getCardinalList() {

		JSONParser parser = new JSONParser();
		JSONObject org_data = null;
		JSONObject common_data = null;

		JSONObject cardinals = new JSONObject();

		try {

			File loc_symtable = new File(this.loc_final_graph);
			BufferedReader br = new BufferedReader(new FileReader(loc_symtable));

			Object obj = parser.parse(br);

			if (obj instanceof JSONObject) {
				org_data = (JSONObject) obj;
				common_data = getCommonData(org_data); // 클래스별 파티션 정보를 불러온다
			}

			if (common_data.size() == 0)
				return null;

			JSONObject links = getLinks(org_data, common_data);

			cardinals.put("common_data", common_data);
			cardinals.put("links", links);

			return cardinals;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	private JSONObject getLinks(JSONObject org_data, JSONObject common_data) {

		JSONObject links_modified = new JSONObject();

		Set<String> set = org_data.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String key = it.next();

			if (!key.equals("micro_detail_partition_by_business_logic"))
				continue;

			JSONObject cardinal_modified = (JSONObject) org_data.get(key);

			Object obj = cardinal_modified.get("links");

			if (obj == null)
				continue;

			JSONArray links = (JSONArray) obj;

			for (int i = 0; i < links.size(); i++) {

				JSONArray sources = new JSONArray();
				JSONObject links_data_org = (JSONObject) links.get(i);
				JSONObject link_modified = new JSONObject();
				JSONObject source_common_data = (JSONObject) common_data.get("" + links_data_org.get("source"));
				
				if(source_common_data == null) {
					System.out.println("links_data_org : " + links_data_org.toJSONString());
					
					continue;
				}
				String source = "" + source_common_data.get("class_full_nm");
				String category = "" + source_common_data.get("category");
				String target = "" + links_data_org.get("target");
				JSONObject target_methods = (JSONObject) links_data_org.get("method");

				if (links_modified.containsKey(target)) {
					sources = (JSONArray) links_modified.get(target);
				}

				link_modified.put("source", source);
				link_modified.put("source_simple", "" + links_data_org.get("source"));
				link_modified.put("target_methods", target_methods);
				link_modified.put("target", target);
				link_modified.put("source_partition", category);

				sources.add(link_modified);
				links_modified.put(target, sources);

			}
		}

		return links_modified;

	}

	private JSONObject getCommonData(JSONObject org_data) {

		JSONObject common_data = new JSONObject();

		Set<String> set = org_data.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String key = it.next();
//			System.out.println(key);
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
				
				if("StreamDownloadView".equals(class_nm)) {
					System.out.println();
				}
				String class_full_nm = "" + class_data.get("filepath");

				class_full_nm = class_full_nm.split(",")[1].trim();
				class_full_nm = class_full_nm.replaceAll(org_src_dir_nm+"/", "");
				String category = "" + class_data.get("category");
				if("Unobserved".equals(category)) {
//					continue;
				}
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

	public JSONObject getCardinalList(String loc_final_graph, String loc_code) {
		this.loc_final_graph = loc_final_graph;
		this.loc_code = loc_code;

		return getCardinalList();
	}

}
