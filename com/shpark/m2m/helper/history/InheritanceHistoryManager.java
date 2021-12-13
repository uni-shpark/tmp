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
public class InheritanceHistoryManager implements Constant {

	private static Properties props = null;
	
	static {

		if (props == null) {
			
			BufferedReader br = null;
			System.out.println(loc_inheritance_history);
			
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_inheritance_history), "UTF-8"));
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
			String parent_nm = ""+data.get("parent_nm");
			String child_nm = ""+data.get("child_nm");
			String new_parent_nm = ""+data.get("new_parent_nm");
			if(!props.containsKey(parent_nm+"@"+child_nm)) {
				props.put(parent_nm+"@"+child_nm, new_parent_nm);
			}
		}
		
		store();
		
		return true;
	}
	
	private void store() {
		
		try (OutputStream output = new FileOutputStream(InheritanceHistoryManager.loc_inheritance_history)) {
			props.store(output, "");
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	
	public boolean delete(JSONArray resultlist) {

//		result.put("new_parent_nm", new_parent_nm);
//		result.put("parent_nm", parent_nm);
//		result.put("child_nm", child_nm);
		
		for (int i = 0; i < resultlist.size(); i++) {
			JSONObject data = (JSONObject)resultlist.get(i);
			String parent_nm = ""+data.get("parent_nm");
			String child_nm = ""+data.get("child_nm");
			
			if(props.containsKey(parent_nm+"@"+child_nm)) {
				props.remove(parent_nm+"@"+child_nm);
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
			
			JSONArray children = new JSONArray();
			
			String key = ""+it.next();
			String parent = key.split("@")[0];
			String child = key.split("@")[1];

			if(!result.containsKey(parent)) {
				children.add(child +"@" + props.getProperty(key));
				result.put(parent, children);
			} else {
				children = (JSONArray)result.get(parent);
				children.add(child +"@" + props.getProperty(key));
				result.put(parent,children);
			}
		}
		
		return result;
	}

	public Properties getProps() {
		
		Properties props = InheritanceHistoryManager.props;
		return props;
		
	}
}
