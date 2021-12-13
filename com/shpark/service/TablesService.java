package com.shpark.service;

import java.util.Iterator;
import java.util.Map;
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
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.helper.DataConversion;
import com.shpark.m2m.helper.history.CardinalHistoryManager;
import com.shpark.m2m.table.TableAnalyzer;
import com.shpark.m2m.table.TableDismantler;

@RestController
public class TablesService implements Constant {

	@Resource
	TableAnalyzer analyzer;

	@Resource
	TableDismantler dismantler;

	@Resource
	CardinalHistoryManager history;
	
	@CrossOrigin("*")
	@RequestMapping(value = "/tableView.do", method = RequestMethod.GET)
	public @ResponseBody JSONArray tableView(HttpServletResponse response) {

		JSONArray resultlist = new JSONArray();

		try {
			resultlist = analyzer.getTableList(loc_final_graph, resultlist, history.read());
			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	@CrossOrigin("*")
	@RequestMapping(value = "/sqlView.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray sqlView(@RequestParam Map<String, String> command, HttpServletResponse response) {

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

//	        class_full_nm: row.class_full_nm,
//	        tables: row.tables,
//	        serviceName: row.serviceName

			result.put("sql", Finder.findSoftlyWithJArraySimple(args).get("sql"));
			resultlist.add(result);

			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	@CrossOrigin("*")
	@RequestMapping(value = "/stackView.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray stackView(@RequestParam Map<String, String> command, HttpServletResponse response) {

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

//	        class_full_nm: row.class_full_nm,
//	        tables: row.tables,
//	        serviceName: row.serviceName

			result.put("stack", Finder.findSoftlyWithJArraySimple(args).get("stack"));
			resultlist.add(result);

			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	@CrossOrigin("*")
	@RequestMapping(value = "/tableAnalyze.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray tableAnalyze(@RequestParam Map<String, String> command,
			HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();

		JSONArray args = null;
		JSONParser parser = new JSONParser();
		JSONObject result = new JSONObject();
		JSONObject resultlist = new JSONObject();

		try {
			while (it.hasNext()) {
				String key = it.next();
				System.out.println(key);
				args = (JSONArray) parser.parse(key);
			}

			if (args == null)
				return null;

//			System.out.println(args.toString());			
			resultlist = dismantler.tableAnalyze(args);

			return DataConversion.tableAnalyze(resultlist);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
}
