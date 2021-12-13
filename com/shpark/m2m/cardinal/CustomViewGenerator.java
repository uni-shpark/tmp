package com.shpark.m2m.cardinal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import com.shpark.m2m.bci.server.lucene.Finder;
import com.shpark.m2m.helper.history.CardinalHistoryManager;
import com.shpark.m2m.util.Configure;

@Component
public class CustomViewGenerator {

	Map<String, String> groupMap = new HashMap<String, String>();
	Map<String, String> validGroupMap = new HashMap<String, String>();

	String loc_final_graph;
	String tagged_clazz;
	String loc_class_run;;
	String workspace;

	String class_nm;
	String loc_org_class_run;

	// final String tag = "OUCMCM902S0, OSCMCM902S0, OCCMCM902S0, UseCounter,
	// UseCountService, UseEntry, OSCMBA721S0, OUCMBA721S0, OSCMBA342S0,
	// OUCMBA342S0, OCCMCM100S1, OSCMCM100S1, OUCMCM100S1, StringUtil, OUCMCM100S0,
	// OCCMCM100S0, CsvRowHandler, SpringUtils, KmtcExceptionView, OCCMCM907S0,
	// OSCMCM907S0, OUCMCM907S0, FileController, OUCMBA311S0, OUSCBS121S0,
	// OSSCBS121S0, SqlMapExtractingSqlMapConfigParser, OCSCBS121S0, BcmDao,
	// HrBatchDao, HrBatchDao2, OUCMBA999S0, FrontDepDao, OCCMBA999S0, OUCMBA311S0,
	// OSCMBA311S0, MonitorService, SyncService, SyncController, KmtcPushSender,
	// KmtcMailSender, OUCMBA310S0, OCCMBA342S0, KmtcMessageDao, StreamDownloadView,
	// OCCMBA366S0, StringUtil, OSCMCM100S0, HtmlMimeMessagePreparator";

	public JSONObject getPartitionNodes(JSONObject micro_detail_partition_by_business_logic) {
		JSONObject overview = (JSONObject) micro_detail_partition_by_business_logic.get("overview");
		JSONArray nodes = (JSONArray) overview.get("nodes");

		for (int i = 0; i < nodes.size(); i++) {

		}

		return null;
	}

	private String usecaseSort(JSONArray semantics) {

		TreeMap<String, String> use_case_map = new TreeMap<String, String>();
		String use_case = "";

		boolean isUnKnown = false;
		for (int j = 0; j < semantics.size(); j++) {

			if ("Unknown".equals(semantics.get(j))) {
				isUnKnown = true;
				continue;
			}
			use_case_map.put("" + semantics.get(j), "");
		}

		Set<String> set = use_case_map.keySet();
		Iterator<String> it = set.iterator();

		StringBuffer sb = new StringBuffer();

		int cnt = 0;
		while (it.hasNext()) {

			if (cnt == 0) {
				sb.append(it.next());
				cnt++;
				continue;
			}
			sb.append("," + it.next());
		}

		return sb.toString();
	}

	public JSONObject sort(JSONArray nodes) {

		JSONObject analyze = new JSONObject();

		for (int i = 0; i < nodes.size(); i++) {
			JSONObject clazz = (JSONObject) nodes.get(i);

			JSONArray semantics = (JSONArray) clazz.get("semantics");

			String clazzNm = "" + clazz.get("FQCN");

			String use_case = usecaseSort(semantics);

			if ("".equals(use_case)) {
				use_case = "Unobserved";
			}
			if (analyze.containsKey(use_case)) {
				clazzNm = clazzNm + "," + analyze.get(use_case);
				analyze.put(use_case, clazzNm);

			} else {
				analyze.put(use_case, clazzNm);
			}
		}

//		Set<String> set = analyze.keySet();
//		Iterator<String> it = set.iterator();
//
//		while (it.hasNext()) {
//			String key = it.next();
//			System.out.println(key);
//			System.out.println(analyze.get(key));
//		}
		return analyze;
	}

	public JSONArray modifyNodes(JSONObject nodes) {

		JSONArray modifyNodes = new JSONArray();

		Set<String> set = nodes.keySet();
		Iterator<String> it = set.iterator();

		int groupId_multi = 1000;
		int groupId = 100;
		while (it.hasNext()) {
			JSONObject modifynode = new JSONObject();

			String key = it.next();
			String clazz = "" + nodes.get(key);

			if (key.indexOf("Unknown") >= 0) {
				System.out.println();
			}

			String[] semantics_str = key.split(",");

			boolean isUnobserved = false;
			if (semantics_str.length == 1) {

				if ("Unobserved".equals(semantics_str[0])) {
					isUnobserved = true;
					modifynode.put("group", 1);
				}

				modifynode.put("group", groupId);
				modifynode.put("name", semantics_str[0]);
				modifynode.put("category", semantics_str[0]);
				groupId++;

			} else {
				modifynode.put("group", groupId_multi);
				modifynode.put("name", "_multi" + groupId_multi);
				modifynode.put("category", "_multi" + groupId_multi);
				groupId_multi++;
			}

			groupMap.put(key, (String) modifynode.get("category") + "&&" + modifynode.get("group"));
			modifynode.put("filepath", "Cluster");

			JSONArray semantics = new JSONArray();

			if (!isUnobserved) {
				for (String use_case : semantics_str) {
					semantics.add(use_case);
				}
			}

			modifynode.put("semantics", semantics);
			modifyNodes.add(modifynode);

		}

//		System.out.println(modifyNodes.toString());
		this.validGroupMap = new HashMap<String, String>(this.groupMap);
		return modifyNodes;
	}

	private void filewrite(File file_loc, String contents) throws IOException {

		File dir = file_loc.getParentFile();
		PrintWriter out = null;

		if (!dir.exists()) {
			dir.mkdirs();
		}
		try {

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_loc), "UTF-8"));
			out = new PrintWriter(bw);

			out.println(contents.toString());

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception e) {

			}
		}
	}

//	public JSONArray getModifiedGroups() {
//
//		String[] groups_split = null;
//		String[] groups_rename = null;
//
//		JSONArray grouplist = new JSONArray();
//		JSONObject groups_split_Map = new JSONObject();
//		JSONObject groups_rename_Map = new JSONObject();
//
//		Configure conf = new Configure();
//
//		if (conf.getProps().getProperty("groups_split") != null) {
//			groups_split = conf.getProps().getProperty("groups_split").split(",");
//		}
//
//		if (conf.getProps().getProperty("groups_rename") != null) {
//			groups_rename = conf.getProps().getProperty("groups_rename").split(",");
//		}
//
//		for (String rename : groups_rename) {
//			groups_rename_Map.put(rename.split(":")[0], rename.split(":")[1]);
//		}
//		for (String split : groups_split) {
//			groups_split_Map.put(split.split(":")[0], split.split(":")[1]);
//		}
//
//		grouplist.add(groups_split_Map);
//		grouplist.add(groups_rename_Map);
//
//		return grouplist;
//	}

	private JSONArray deleteUnUsedNodes(JSONArray modifyNodes) {

		Set<String> set = this.validGroupMap.keySet();
		Iterator<String> it = set.iterator();

		JSONArray removeNodes = new JSONArray();

		while (it.hasNext()) {
			String usecase = it.next();
			String nodeNm = this.validGroupMap.get(usecase).split("&&")[0];
			for (int i = 0; i < modifyNodes.size(); i++) {

				JSONObject node = (JSONObject) modifyNodes.get(i);
				String name = usecaseSort((JSONArray) node.get("semantics"));
				if (usecase.equals(name)) {
					removeNodes.add(node);
				}
			}

		}

		for (int i = 0; i < removeNodes.size(); i++) {

			JSONObject removeNode = (JSONObject) removeNodes.get(i);
			modifyNodes.remove(removeNode);

		}

		for (int i = 0; i < modifyNodes.size(); i++) {

			JSONObject node = (JSONObject) modifyNodes.get(i);
			String name = "" + node.get("name");
			String semantics = ((JSONArray) node.get("semantics")).toJSONString();
			System.out.println(name + "\t" + semantics);

		}
		return modifyNodes;

	}

	private String getSemantics(JSONArray semantics) {

		String groupMapKey = "";
		boolean isUnKnown = false;

		for (int j = 0; j < semantics.size(); j++) {

			if ("Unknown".equals(semantics.get(j))) {
				isUnKnown = true;
				continue;
			}

			if (j > 0) {
				if (!(j == 1 && isUnKnown))
					groupMapKey += ",";
			}

			groupMapKey += semantics.get(j);
		}

		return groupMapKey;

	}

	public String getLocalLogs() {

		StringBuffer logs_sb = new StringBuffer();

		File worksdir = new File(this.workspace + "/logs");

		File[] files = worksdir.listFiles();

		int cnt = 0;

		for (int i = 0; i < files.length; i++) {

			if (files[i].getName().endsWith("_cleaned"))
				continue;

			if (cnt == 0) {
				logs_sb.append(files[i].getName());
			} else {
				logs_sb.append("," + files[i].getName());
			}
			cnt++;
		}
		return logs_sb.toString();
	}

	private JSONArray modifyClazz(JSONArray org_data) {

		JSONArray modifyClazzList = new JSONArray();
		CardinalHistoryManager history = new CardinalHistoryManager();

		JSONArray history_data = new JSONArray();

		for (int i = 0; i < org_data.size(); i++) {

			JSONObject modifying = (JSONObject) org_data.get(i);

			String groupMapKey = "";
			String name = "" + modifying.get("name");
			int dupidx = name.indexOf("[Duplicate_#");

			if (dupidx > 0) {
				name = name.substring(0, dupidx).trim();
			}
			if (name.startsWith("StringUtil")) {
				System.out.println();
			}

			if (name.equals("OSCMBA316S0")) {
				System.out.println();
			}
			JSONArray semantics = (JSONArray) modifying.get("semantics");

			groupMapKey = usecaseSort(semantics);

			if (this.tagged_clazz.indexOf(name) >= 0) {
				String bak_category = this.groupMap.get(groupMapKey);

				if (bak_category != null) {
					JSONObject back_data = new JSONObject();
					back_data.put("class_nm", "" + modifying.get("FQCN"));

					String sd = history.getProps().getProperty("" + modifying.get("FQCN"));
					String logs = "";
					if (sd == null) {
						logs = getLocalLogs();
					} else {
						logs = history.getProps().getProperty("" + modifying.get("FQCN")).split("&&")[0];
					}
					back_data.put("bak_file", logs + "&&" + this.groupMap.get(groupMapKey).split("&&")[0]);
					history_data.add(back_data);
				}
				groupMapKey = "Unobserved";
				semantics = new JSONArray();
				modifying.put("semantics", semantics);

			}

			if ("".equals(groupMapKey)) {
				groupMapKey = "Unobserved";
			}

			String[] modifying_data = new String[2];
			if (this.groupMap.containsKey(groupMapKey)) {
				modifying_data = this.groupMap.get(groupMapKey).split("&&");

				if (this.validGroupMap.containsKey(groupMapKey)) {
					this.validGroupMap.remove(groupMapKey);
				}

			} else {
				System.out.println("unknown groupMapId : " + groupMapKey);
				continue;
			}

			modifying.put("category", modifying_data[0]);
			modifying.put("group", modifying_data[1]);

			modifyClazzList.add(modifying);
		}

		history.write(history_data);
		return modifyClazzList;
	}

	public CustomViewGenerator() {
	}

	public void grouping(String loc_final_graph, Properties tagged_list, String loc_classrun, String workspace) {

		this.loc_final_graph = loc_final_graph;
		this.tagged_clazz = tagging(tagged_list);
		this.loc_class_run = loc_classrun;
		this.workspace = workspace;

		move();
	}

	private String tagging(Properties tagged_list) {

		Set set = tagged_list.keySet();
		Iterator it = set.iterator();

		StringBuffer tag = new StringBuffer();

		int cnt = 0;
		while (it.hasNext()) {

			String clazz_full = "" + it.next();
			String clazz = clazz_full.substring(clazz_full.lastIndexOf(".") + 1, clazz_full.length());

			if (cnt == 0) {
				tag.append(clazz);
			} else {
				tag.append("," + clazz);
			}
			cnt++;
		}

		if (cnt > 0) {
			tag.append("," + Configure.getProps().getProperty("commons"));
		} else {
			tag.append(Configure.getProps().getProperty("commons"));
		}

		return tag.toString();
	}

	private JSONArray classSemanticsData(String clazzNm, JSONObject bizPartition) {

		try {
			JSONArray nodes = (JSONArray) bizPartition.get("nodes");

			JSONArray class_semantics = getClazzSemantics(nodes, clazzNm);

			return class_semantics;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private JSONObject classData(String clazzNm, JSONObject bizPartition) {

		JSONArray modifyNodes = null;
		JSONObject finalgraph_obj = null;

		try {
			JSONArray nodes = (JSONArray) bizPartition.get("nodes");

			JSONObject clazz = getClass(nodes, clazzNm);

			return clazz;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private JSONObject getClass(JSONArray nodes, String clazzNm) {

		for (int i = 0; i < nodes.size(); i++) {

			JSONObject modifying = (JSONObject) nodes.get(i);

			String FQCN = "" + modifying.get("FQCN");
//			String name = "" + modifying.get("name");
//			int dupidx = name.indexOf("[Duplicate_#");
//
//			if (dupidx > 0) {
//				name = name.substring(0, dupidx).trim();
//			}

			if (FQCN.equals(clazzNm)) {
				return modifying;
			}
		}
		return null;
	}

	private JSONArray getClazzSemantics(JSONArray nodes, String clazzNm) {

		for (int i = 0; i < nodes.size(); i++) {

			JSONObject modifying = (JSONObject) nodes.get(i);

			String FQCN = "" + modifying.get("FQCN");
//			String name = "" + modifying.get("name");
//			int dupidx = name.indexOf("[Duplicate_#");
//
//			if (dupidx > 0) {
//				name = name.substring(0, dupidx).trim();
//			}

			if (FQCN.equals(clazzNm)) {
				JSONArray semantics = (JSONArray) modifying.get("semantics");

				return semantics;
			}
		}
		return null;
	}

	private void move() {

		classrunClean();

		JSONParser parser = new JSONParser();
		BufferedReader br = null;

		JSONArray modifyNodes = null;
		JSONObject finalgraph_obj = null;

		try {

			br = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(this.loc_final_graph)), "UTF-8"));

			finalgraph_obj = (JSONObject) parser.parse(br);
			JSONObject micro_detail_partition_by_business_logic = (JSONObject) finalgraph_obj
					.get("micro_detail_partition_by_business_logic");
			JSONArray org_data = (JSONArray) micro_detail_partition_by_business_logic.get("nodes");

			modifyNodes = modifyNodes(sort(org_data));
			JSONArray class_partition = modifyClazz(org_data);
			micro_detail_partition_by_business_logic.put("nodes", class_partition);

			JSONObject overview = (JSONObject) micro_detail_partition_by_business_logic.get("overview");
			overview.put("nodes", modifyNodes);
			micro_detail_partition_by_business_logic.put("overview", overview);

			finalgraph_obj.put("micro_detail_partition_by_business_logic", micro_detail_partition_by_business_logic);

			modifyNodes = deleteUnUsedNodes(modifyNodes);

			filewrite(new File(loc_final_graph), finalgraph_obj.toJSONString());

			readMulti(finalgraph_obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		String loc_final_graph = "E:\\Work\\SITE\\KMTC\\uma_workspace\\files\\final_graph.json";
		String loc_org_final_graph = "E:\\Work\\SITE\\KMTC\\uma_workspace\\files\\org\\final_graph.json";

		CustomViewGenerator custom = new CustomViewGenerator();
		JSONObject final_obj = custom.getFinalData(loc_final_graph);
		JSONObject org_final_obj = custom.getFinalData(loc_org_final_graph);
		JSONObject bizPartition = custom.getBizPartition(final_obj);
		JSONArray org_sema = custom.classSemanticsData("com.icc.cm.ba.CMBA342.svc.OCCMBA342S0",
				custom.getBizPartition(org_final_obj));

		JSONObject clazz_obj = custom.classData("com.icc.cm.ba.CMBA342.svc.OCCMBA342S0", bizPartition);
		bizPartition = custom.rollback(org_sema, bizPartition, clazz_obj);
		final_obj.put("micro_detail_partition_by_business_logic", bizPartition);

		custom.filewrite(new File(loc_final_graph), final_obj.toJSONString());
//		custom.readMulti(finalgraph_obj);

	}

	public boolean codeBak(JSONArray args, String loc_final_graph, String loc_org_final_graph, String loc_class_run,
			String loc_org_class_run, Properties tagged_list) {

//		String loc_final_graph = "E:\\Work\\SITE\\KMTC\\uma_workspace\\files\\final_graph.json";
//		String loc_org_final_graph = "E:\\Work\\SITE\\KMTC\\uma_workspace\\files\\org\\final_graph.json";

		try {
			
			String clazz = "" + ((JSONObject) args.get(0)).get("class_nm");
			
			String tag = tagging(tagged_list);
			classRunBak(clazz, tag, loc_class_run, loc_org_class_run);

			CustomViewGenerator custom = new CustomViewGenerator();
			JSONObject final_obj = custom.getFinalData(loc_final_graph);
			custom.filewrite(new File(loc_final_graph), final_obj.toJSONString());
			JSONObject org_final_obj = custom.getFinalData(loc_org_final_graph);
			JSONObject bizPartition = custom.getBizPartition(final_obj);
			JSONArray org_sema = custom.classSemanticsData(clazz, custom.getBizPartition(org_final_obj));

			JSONObject clazz_obj = custom.classData(clazz, bizPartition);
			bizPartition = custom.rollback(org_sema, bizPartition, clazz_obj);
			final_obj.put("micro_detail_partition_by_business_logic", bizPartition);

			custom.filewrite(new File(loc_final_graph), final_obj.toJSONString());
//		custom.readMulti(finalgraph_obj);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	private JSONObject getFinalData(String loc_final_graph) {

		JSONParser parser = new JSONParser();
		BufferedReader br = null;

		JSONArray modifyNodes = null;
		JSONObject finalgraph_obj = null;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(loc_final_graph)), "UTF-8"));

			finalgraph_obj = (JSONObject) parser.parse(br);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return finalgraph_obj;

	}

	private JSONObject getBizPartition(JSONObject final_data) {

		JSONObject micro_detail_partition_by_business_logic = null;

		try {
			micro_detail_partition_by_business_logic = (JSONObject) final_data
					.get("micro_detail_partition_by_business_logic");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return micro_detail_partition_by_business_logic;

	}

	private JSONArray setSemantics(JSONArray semantics) {

		JSONArray newSemantics = new JSONArray();
		for (int i = 0; i < semantics.size(); i++) {

			if ("Unknown".equals(semantics.get(i)))
				continue;

			newSemantics.add(semantics.get(i));

		}

		return newSemantics;
	}

	private JSONObject rollback(JSONArray semantics, JSONObject bizPartition, JSONObject clazz_obj) {

		JSONArray modifyNodes = null;
		JSONObject finalgraph_obj = null;

		try {

			JSONObject overview = (JSONObject) bizPartition.get("overview");
			JSONArray class_nodes = (JSONArray) bizPartition.get("nodes");
			int cdcd1 = class_nodes.size();
			class_nodes.remove(clazz_obj);
			int cdcd2 = class_nodes.size();
			JSONArray nodes = (JSONArray) overview.get("nodes");
			String usecase = usecaseSort(semantics);

			int usecase_len = usecase.split(",").length;

			int groupid = 0;
			int groupid_multi = 0;

			boolean isChecked = false;
			JSONObject source_partition = null;

			for (int i = 0; i < nodes.size(); i++) {

				source_partition = (JSONObject) nodes.get(i);
				String usecase_to = usecaseSort((JSONArray) source_partition.get("semantics"));
				int group = Integer.parseInt("" + source_partition.get("group"));
				int sss = usecase_to.split(",").length;

				if (sss == 1) {
					if (groupid < group)
						groupid = group;
				} else if (sss > 1) {
					if (groupid_multi < group)
						groupid_multi = group;
				}
				if (usecase.equals(usecase_to)) {
					isChecked = true;
					break;
				}
			}

			if (isChecked) {
				clazz_obj.put("group", source_partition.get("group"));
				clazz_obj.put("category", source_partition.get("category"));
				clazz_obj.put("semantics", setSemantics(semantics));
			}
			if (!isChecked) {
				JSONObject partition = new JSONObject();
				partition.put("filepath", "Cluster");
				if (usecase_len > 1) {
					groupid_multi = groupid_multi + 1;
					partition.put("name", "_multi" + groupid_multi);
					partition.put("group", groupid_multi);
					partition.put("category", "_multi" + groupid_multi);
					clazz_obj.put("group", groupid_multi);
					clazz_obj.put("category", "_multi" + groupid_multi);

				} else {
					groupid = groupid + 1;
					partition.put("name", usecase);
					partition.put("group", groupid);
					partition.put("category", usecase);
					clazz_obj.put("group", groupid);
					clazz_obj.put("category", usecase);
				}
				partition.put("semantics", setSemantics(semantics));
				clazz_obj.put("semantics", setSemantics(semantics));

				nodes.add(partition);
				overview.put("nodes", nodes);
				bizPartition.put("overview", overview);

				int cdcd3 = class_nodes.size();
				System.out.println();
			}
			class_nodes.add(clazz_obj);
			bizPartition.put("nodes", class_nodes);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return bizPartition;

	}

	private JSONObject getDependency(JSONObject finalgraph_obj) {

		JSONObject mono_data_dependency_graph = (JSONObject) finalgraph_obj.get("mono_data_dependency_graph");
		JSONArray links = (JSONArray) mono_data_dependency_graph.get("links");

		JSONObject dependency = new JSONObject();

		for (int i = 0; i < links.size(); i++) {
			JSONObject link = (JSONObject) links.get(i);

			String source = "" + link.get("source");
			String target = "" + link.get("target");

			if (dependency.containsKey(source)) {

				String target_classes = "" + dependency.get(source);
				target_classes += "," + target;
				dependency.put(source, target_classes);

			} else {
				dependency.put(source, target);
			}

			if (dependency.containsKey(target)) {

				String source_classes = "" + dependency.get(target);
				source_classes += "," + source;
				dependency.put(target, source_classes);

			} else {
				dependency.put(target, source);
			}

		}

		Set<String> set = dependency.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {
			String key = it.next();

			if ("OUCSCM100S0".equals(key)) {
				String ss = "" + dependency.get(key);
				System.out.println();
			}
		}
		return dependency;
	}

	private void readMulti(JSONObject finalgraph_obj) {
		Set<String> set = finalgraph_obj.keySet();
		Iterator<String> it = set.iterator();

		JSONObject depenency = getDependency(finalgraph_obj);
		JSONObject micro_detail_partition_by_business_logic = (JSONObject) finalgraph_obj
				.get("micro_detail_partition_by_business_logic");

		JSONArray org_data = (JSONArray) micro_detail_partition_by_business_logic.get("nodes");

		JSONObject multi = new JSONObject();
		JSONObject partitionMap = new JSONObject();

		JSONObject analyze = new JSONObject();

		JSONObject FQCNMAP = new JSONObject();
		for (int i = 0; i < org_data.size(); i++) {
			JSONObject clazz = (JSONObject) org_data.get(i);

			String partition = "" + clazz.get("category");
			String FQCN = "" + clazz.get("FQCN");
			String name = "" + clazz.get("name");
			FQCNMAP.put(name, FQCN);

			partitionMap.put(name, partition);
			if (partition.startsWith("_multi")) {

				if (multi.containsKey(partition)) {
					String class_list = "" + multi.get(partition);
					class_list += "," + name;
					multi.put(partition, class_list);
				} else {
					multi.put(partition, name);
				}
			}
			boolean isUnKnown = false;
		}

		Set<String> multi_set = multi.keySet();
		Iterator<String> multi_it = multi_set.iterator();
		while (multi_it.hasNext()) {
			String key = multi_it.next();
			String classes = "" + multi.get(key);
			String[] classlist = classes.split(",");

			String dep = "";
			String serviceName = "";
			int cnt_srv = 0;
			JSONObject servicetmp = new JSONObject();
			for (int i = 0; i < classlist.length; i++) {
				if (i == 0) {
					dep = "" + depenency.get(classlist[i]);
				} else {
					dep += "," + depenency.get(classlist[i]);
				}

//				Finder finder = new Finder();

				String[] deps = dep.split(",");
				String partitions = "";

				int cnt = 0;
				Map<String, String> tmp = new HashMap<String, String>();

				for (int j = 0; j < deps.length; j++) {

					String partition_tmp = "" + partitionMap.get(deps[j]);

					if (partition_tmp.equals(key) || "Unobserved".equals(partition_tmp)) {
						continue;
					}

					if (tmp.containsKey(partition_tmp)) {
						continue;
					} else {
						tmp.put(partition_tmp, partition_tmp);
					}
					if (cnt == 0) {
						partitions = partition_tmp;
						cnt++;
					} else {
						partitions += "," + partition_tmp;
						cnt++;
					}

				}

				String ccccc = "" + FQCNMAP.get(classlist[i]);
				List<Document> docs = Finder.findSoftly("" + FQCNMAP.get(classlist[i]));

				for (int j = 0; j < docs.size(); j++) {
					String custom_stacks = docs.get(j).get("custom_stacks");
					if (custom_stacks.indexOf(ccccc) < 0)
						continue;

					if (servicetmp.containsKey(docs.get(j).get("serviceName"))) {
						continue;
					} else {
						servicetmp.put(docs.get(j).get("serviceName"), "");
					}
					if (cnt_srv == 0) {
						serviceName = docs.get(j).get("serviceName");
						cnt_srv++;
					} else {
						serviceName += ","+docs.get(j).get("serviceName");
						cnt_srv++;
					}
				}

				multi.put(key, classes + "\t" + partitions+ "\t" + serviceName);
			}
		}

		Set<String> resultset = multi.keySet();
		Iterator<String> resultIt = resultset.iterator();
		while (resultIt.hasNext()) {

			String key = resultIt.next();
			System.out.println(key + "\t" + multi.get(key));
		}
	}

	public void classrunClean() {
		String loc_classrun_modify = this.loc_class_run;

		JSONParser parser = new JSONParser();
		BufferedReader br = null;

		JSONArray modifyNodes = null;

		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.loc_class_run)), "UTF-8"));

			JSONObject class_run_obj = (JSONObject) parser.parse(br);

			System.out.println();

			Set<String> set = class_run_obj.keySet();
			Iterator<String> it = set.iterator();

			while (it.hasNext()) {

				String key = it.next();
				JSONObject clazz = (JSONObject) class_run_obj.get(key);

				JSONObject from = deleteClassLog((JSONObject) clazz.get("From"));
				clazz.put("From", from);
				JSONObject to = deleteClassLog((JSONObject) clazz.get("To"));
				clazz.put("To", to);

				class_run_obj.put(key, clazz);

			}

			String[] tages = this.tagged_clazz.split(",");

			for (String tag : tages) {
				class_run_obj.remove(tag.trim());
			}
			filewrite(new File(loc_classrun_modify), class_run_obj.toJSONString());
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void classRunBak(String class_nm, String tagged_clazz, String loc_class_run, String loc_org_class_run) {

		this.class_nm = class_nm;
		this.tagged_clazz = tagged_clazz;
		this.loc_class_run = loc_class_run;
		this.loc_org_class_run = loc_org_class_run;
		
		classRunBak();
	}

	private void classRunBak() {

		JSONParser parser = new JSONParser();
		BufferedReader br = null;

		JSONArray modifyNodes = null;

		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(loc_org_class_run)), "UTF-8"));

			JSONObject class_run_obj = (JSONObject) parser.parse(br);

			System.out.println();

			Set<String> set = class_run_obj.keySet();
			Iterator<String> it = set.iterator();

			while (it.hasNext()) {

				String key = it.next();
				JSONObject clazz = (JSONObject) class_run_obj.get(key);

				JSONObject from = deleteClassLog((JSONObject) clazz.get("From"));
				clazz.put("From", from);
				JSONObject to = deleteClassLog((JSONObject) clazz.get("To"));
				clazz.put("To", to);

				class_run_obj.put(key, clazz);

			}

			String[] tages = tagged_clazz.split(",");
			class_nm = class_nm.substring(class_nm.lastIndexOf(".") + 1, class_nm.length());
			for (String tag : tages) {
				if (!tag.equals(class_nm)) {
					class_run_obj.remove(tag.trim());
				}
			}
			filewrite(new File(loc_class_run), class_run_obj.toJSONString());
			System.out.println();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JSONObject deleteClassLog(JSONObject fromTo) {

		Set<String> set = fromTo.keySet();
		Iterator<String> it = set.iterator();

		JSONObject from_tmp = new JSONObject(fromTo);

		while (it.hasNext()) {
			String key = it.next();
			if (this.tagged_clazz.indexOf(key) >= 0) {
				from_tmp.remove(key);
			}
		}

		return from_tmp;
	}
}
