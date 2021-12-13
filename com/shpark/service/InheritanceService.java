package com.shpark.service;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.cglib.core.Constants;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.helper.CodeView;
import com.shpark.m2m.helper.DataConversion;
import com.shpark.m2m.helper.history.InheritanceHistoryManager;
import com.shpark.m2m.inheritance.InheritanceAnalyzer;
import com.shpark.m2m.inheritance.InheritanceDismantler;

@RestController
public class InheritanceService implements Constant {

	@Resource
	InheritanceAnalyzer analyzer;
	
	@Resource
	InheritanceDismantler dismantler;

	@Resource
	InheritanceHistoryManager history;
	
	@CrossOrigin("*")
	@RequestMapping(value = "/inheritance.do", method = RequestMethod.GET)
	public @ResponseBody JSONArray getInheritance(HttpServletResponse response) {
		JSONObject inheritance = new JSONObject();
		JSONArray final_data = new JSONArray();

		inheritance = analyzer.getInheritanceList(loc_symtable, loc_code);

		if (inheritance.size() < 1) {
			System.out.println("변환 대상이 존재하지 않습니다.");
		}

		JSONObject history_obj = null;
		JSONObject inheritance_obj = null;
		history_obj = history.read();

		inheritance_obj = DataConversion.inheritanceMerge(inheritance, history_obj, null);
		final_data = DataConversion.inheritanceConversion(inheritance_obj, loc_code);

		System.out.println(final_data.toJSONString());

		return final_data;
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/codeView.do", method = RequestMethod.POST)
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

			result.put("parent", CodeView.load("" + args.get("parent"), loc_code));
			result.put("child", CodeView.load("" + args.get("child"), loc_code));
//			
			resultlist.add(result);

			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	@CrossOrigin("*")
	@RequestMapping(value = "/genCode.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray codeGen(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();
		JSONParser parser = new JSONParser();
		JSONObject args = null;
		Properties history_props = history.getProps();

		JSONArray result = new JSONArray();

		JSONObject inheritance = analyzer.getInheritanceList(loc_symtable, loc_code);

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONObject) parser.parse(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (args != null) {
			result = dismantler.dismantle(history_props, args, null, loc_code);
		}

		JSONObject history_obj = null;
		JSONObject inheritance_obj = null;
		if (history.write(result)) {
			history_obj = history.read();
		}

		inheritance_obj = DataConversion.inheritanceMerge(inheritance, history_obj, result);
		JSONArray final_data = DataConversion.inheritanceConversion(inheritance_obj);

		System.out.println(final_data.toJSONString());

		return final_data;
	}

	
	@CrossOrigin("*")
	@RequestMapping(value = "/bakCode.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray codeBak(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();
		JSONParser parser = new JSONParser();
		JSONObject args = null;
		Properties history_props = history.getProps();
		
		JSONArray result = new JSONArray();

		JSONObject inheritance = analyzer.getInheritanceList(loc_symtable, loc_code);

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONObject) parser.parse(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		result = dismantler.rollback(history_props, args, loc_code);
		
		JSONObject history_obj = null;
		JSONObject inheritance_obj = null;
		
		if(history.delete(result))
			history_obj = history.read();
		
		inheritance_obj = DataConversion.inheritanceMerge(inheritance, history_obj, result);
		JSONArray final_data = DataConversion.inheritanceConversion(inheritance_obj);
		
		System.out.println(final_data.toJSONString());

		return final_data;
		
	}
}
