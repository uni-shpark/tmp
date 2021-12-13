package com.shpark.service;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.contents.ContentsAnalyzer;
import com.shpark.m2m.helper.CodeView;
import com.shpark.m2m.helper.Commons;
import com.shpark.m2m.helper.DataConversion;

@RestController
public class ContentsService implements Constant {

	@Resource
	ContentsAnalyzer helper;

	@Resource
	Commons commons;

	@CrossOrigin("*")
	@RequestMapping(value = "/contents.do", method = RequestMethod.GET)
	public @ResponseBody JSONArray getContents(HttpServletResponse response) {

		JSONArray resultlist = new JSONArray();
		resultlist = DataConversion.contentsConversion(helper.find(loc_code));

		return resultlist;

	}

	@CrossOrigin("*")
	@RequestMapping(value = "/getPartitions.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray getPartitions(HttpServletResponse response) {

		JSONParser parser = new JSONParser();
		JSONArray result = new JSONArray();

		result = DataConversion.partitionsconversion(commons.getPartitions());
		System.out.println(result.toJSONString());
		return result;

	}

	@CrossOrigin("*")
	@RequestMapping(value = "/contentsView.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray getContent(@RequestParam Map<String, String> command, HttpServletResponse response) {

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

			result.put("name", CodeView.load("" + args.get("name"), null));
			resultlist.add(result);

			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
	
	@CrossOrigin("*")
	@RequestMapping(value = "/contentsCopy.do", method = RequestMethod.POST)
	public @ResponseBody JSONArray contentsCopy(@RequestParam Map<String, String> command, HttpServletResponse response) {

		Set<String> set = command.keySet();
		Iterator<String> it = set.iterator();

		JSONObject args = null;
		JSONParser parser = new JSONParser();
		JSONArray resultlist = new JSONArray();

		try {
			while (it.hasNext()) {
				String key = it.next();
				args = (JSONObject) parser.parse(key);
			}

			if (args == null)
				return resultlist;

			System.out.println(args.toString());
			
			// TODO
			// args 파싱 partitions -> JSONArray 데이터 확인
			// args 파싱 name -> String 데이터 확인
			// JSONArray에 있는 파티션 처음부터 끝까지 차례차례 확인 하는 로직
			
			//COPY -> CardinalDismantler.classCopy를 참조
			
			return resultlist;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
}
