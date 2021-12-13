package com.shpark.m2m.helper.history;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import com.shpark.m2m.constant.Constant;

@Component
public class CardinalHistoryManager implements Constant {

	private static Properties props = null;
	
	static {

		if (props == null) {

			BufferedReader br = null;
			System.out.println(loc_cardinal_history);
			
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_cardinal_history), "UTF-8"));
				props = new Properties();
				props.load(br);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean write(JSONArray resultlist) {

		for (int i = 0; i < resultlist.size(); i++) {
			JSONObject data = (JSONObject)resultlist.get(i);
			
			
			if(data.containsKey("error")) {
				continue;
			}
			String bak_file = ""+data.get("bak_file");
			String class_nm = ""+data.get("class_nm");
			
//			if(!props.containsKey(class_nm)) {
				props.put(class_nm, bak_file);
//			}
			
		}
		store();
		return true;
	}
	
	private void store() {
		
		try (OutputStream output = new FileOutputStream(loc_cardinal_history)) {
			props.store(output, "");
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	
	public boolean delete(JSONArray resultlist) {
		
		for (int i = 0; i < resultlist.size(); i++) {
			JSONObject data = (JSONObject)resultlist.get(i);
			String copy_file = ""+data.get("class_nm");
			
			if(props.containsKey(copy_file)) {
				props.remove(copy_file);
			}
		}
		store();
		
		return true;
	}
	
	public JSONObject read() {
		
		Set<Object> set = props.keySet();
		Iterator<Object> it = set.iterator();
		
		JSONObject result = new JSONObject();
		while (it.hasNext()) {
			
			String key = ""+it.next();

			if(!result.containsKey(key)) {
				result.put(key, props.get(key));
			}
		}
		
		return result;
	}

	public Properties getProps() {
		
		Properties props = CardinalHistoryManager.props;
		return props;
		
	}
}
