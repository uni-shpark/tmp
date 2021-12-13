package com.shpark.service;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.shpark.m2m.bci.server.lucene.Finder;
import com.shpark.m2m.cardinal.CardinalAnalyzer;
import com.shpark.m2m.cardinal.CardinalDismantler;
import com.shpark.m2m.cardinal.CustomViewGenerator;
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.helper.CodeView;
import com.shpark.m2m.helper.DataConversion;
import com.shpark.m2m.helper.RefView;
import com.shpark.m2m.helper.history.CardinalHistoryManager;

@RestController
public class CardinalService implements Constant {

	@Resource
	CardinalAnalyzer analyzer;

	@Resource
	CardinalDismantler dismantler;
	
	@Resource
	CustomViewGenerator c_dismantler;

	@Resource
	CardinalHistoryManager history;

	@CrossOrigin("*")
	@RequestMapping(value = "/cardinal.do", method = RequestMethod.GET)
	public @ResponseBody JSONArray getCardinal(HttpServletResponse response) {
		JSONObject cardinal = new JSONObject();
		JSONArray final_data = new JSONArray();

		cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);

		if (cardinal == null || cardinal.size() < 1) {
			System.out.println("나열 대상이 존재하지 않습니다.");
		}

		JSONObject common_data = (JSONObject) cardinal.get("common_data");
		JSONObject links = (JSONObject) cardinal.get("links");

		JSONObject history_data = history.read();
		JSONObject cardinal_modified = DataConversion.cardinalMerge(common_data, links, null);
		final_data = DataConversion.cardinalConversion(cardinal_modified, history_data);

		//System.out.println(final_data.toJSONString());

		return final_data;
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/linkCodeView.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray codeView(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();

		JSONObject args = null;
		JSONParser parser = new JSONParser();
		JSONObject result = new JSONObject();
		JSONArray resultlist = new JSONArray();

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONObject) parser.parse(key);
			}

			if (args == null)
				return resultlist;

			System.out.println(args.toString());

			String full_nm = "" + args.get("parent");

			String source_partition = "" + args.get("source_partition");

			if (!"commons".equals(source_partition)) {
				source_partition = "src-" + source_partition;
			}

			String target_partition = "" + args.get("target_partition");

			if (!"commons".equals(target_partition)) {
				target_partition = "src-" + target_partition;
			}

			String target_loc = loc_msa_code + target_partition;

			String modify = full_nm.replace(".", "/") + ".java";
			File target_file = new File(target_loc + "/" + modify);

			if (target_file.exists()) {
				result.put("parent", CodeView.load("" + args.get("parent"), loc_msa_code + target_partition));
			} else {
				result.put("parent", CodeView.load("" + args.get("parent"), loc_msa_code + source_partition));
			}

			result.put("child", CodeView.load("" + args.get("child"), loc_msa_code + source_partition));
//			
			resultlist.add(result);

			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/refView.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray refView(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();

		JSONObject args = null;
		JSONParser parser = new JSONParser();
		JSONArray result = new JSONArray();

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONObject) parser.parse(key);
			}

			if (args == null)
				return result;

			String target = "" + args.get("target");
			target = target.substring(target.lastIndexOf(".") + 1);

			String source = "" + args.get("source");
			source = source.substring(source.lastIndexOf(".") + 1);

			JSONObject cardinal = new JSONObject();

			cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);
			JSONObject links = (JSONObject) cardinal.get("links");
			JSONArray link = (JSONArray) links.get(target);

			System.out.println(args.toString());

			result = RefView.load(link, source);

			System.out.println(result.toJSONString());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/cardinalSync.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray sync(@RequestParam Map<String, String> command, HttpServletResponse response) {

		JSONArray result = new JSONArray();

		JSONObject cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);

		JSONObject common_data = (JSONObject) cardinal.get("common_data");
		JSONObject links = (JSONObject) cardinal.get("links");

		try {

//			dismantler.cardinalCleanUp(args, loc_logs, loc_code, loc_msa_code);
			result = dismantler.cardinalSync(history.getProps(), loc_code, loc_msa_code, loc_logs);

			JSONObject history_obj = null;
			JSONObject cardinal_obj = null;
			
			if (history.write(result))
				history_obj = history.read();

			cardinal_obj = DataConversion.cardinalMerge(common_data, links, result);
			JSONArray final_data = DataConversion.cardinalConversion(cardinal_obj, history_obj);

			return final_data;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/copyCode.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray copyCode(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();
		JSONParser parser = new JSONParser();
		JSONArray args = null;
		Properties history_props = history.getProps();

		JSONArray result = new JSONArray();

		JSONObject cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);

		JSONObject common_data = (JSONObject) cardinal.get("common_data");
		JSONObject links = (JSONObject) cardinal.get("links");

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONArray) parser.parse(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// LOG 파일 삭제
		// src 디렉토리에서 commons로 COPY

		if (args != null) {
			result = dismantler.cardinalCleanUp(args, loc_logs, loc_code, loc_msa_code);
		}

		JSONObject history_obj = null;
		JSONObject cardinal_obj = null;
		if (history.write(result))
			history_obj = history.read();

		cardinal_obj = DataConversion.cardinalMerge(common_data, links, result);
		JSONArray final_data = DataConversion.cardinalConversion(cardinal_obj, history_obj);

		return final_data;

	}

	@CrossOrigin("*")
	@RequestMapping(value = "/grouping.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray grouping(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();
		JSONObject cardinal = new JSONObject();
		JSONArray final_data = new JSONArray();
		
		c_dismantler.grouping(loc_final_graph, history.getProps(), loc_class_run, workspace);
		
		cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);

		if (cardinal == null || cardinal.size() < 1) {
			System.out.println("나열 대상이 존재하지 않습니다.");
		}

		JSONObject common_data = (JSONObject) cardinal.get("common_data");
		JSONObject links = (JSONObject) cardinal.get("links");

		JSONObject history_data = history.read();
		JSONObject cardinal_modified = DataConversion.cardinalMerge(common_data, links, null);
		final_data = DataConversion.cardinalConversion(cardinal_modified, history_data);

//		System.out.println(final_data.toJSONString());
		
		return final_data;

	}
	
	@CrossOrigin("*")
	@RequestMapping(value = "/copyBak.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray codeBak(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();
		JSONParser parser = new JSONParser();
		JSONArray args = null;
		Properties history_props = history.getProps();

		JSONArray result = new JSONArray();
		
		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONArray) parser.parse(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		c_dismantler.codeBak(args, loc_final_graph, loc_org_final_graph, loc_class_run, loc_org_class_run, history.getProps());
		JSONObject cardinal = analyzer.getCardinalList(loc_final_graph, loc_code);

		JSONObject common_data = (JSONObject) cardinal.get("common_data");
		JSONObject links = (JSONObject) cardinal.get("links");
		
		result = dismantler.rollback(history_props, args, loc_msa_code, loc_logs);
		// (Properties history_props, JSONArray cardinal, String loc_code)

		JSONObject history_obj = null;
		JSONObject cardinal_obj = null;

		if (history.delete(result))
			history_obj = history.read();

		cardinal_obj = DataConversion.cardinalMerge(common_data, links, result);
		JSONArray final_data = DataConversion.cardinalConversion(cardinal_obj, history_obj);

		System.out.println(final_data.toJSONString());

		return final_data;

	}
}
