package com.shpark.m2m.contents;

import java.io.File;

import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;

@Component
public class ContentsAnalyzer {

	String loc_code;
	
	public JSONArray find(String loc_code) {
		
		this.loc_code = loc_code;
		
		return fine();
	}

	private JSONArray fine() {
		
		return fine(null, null);
		
	}
	private JSONArray fine(String currrent_loc, JSONArray contents) {
		
		if(contents == null) {
			contents = new JSONArray();
		}
				
		if(currrent_loc == null) {
			currrent_loc = this.loc_code;
		}
		File files = new File(currrent_loc);
		if(files.isDirectory()) {
			
			File [] sub_dir = files.listFiles();
			
			for (File file : sub_dir) {
				
//				File file = new File(dir);
				if (!file.isDirectory() && !file.toString().endsWith(".java")) {
					continue;
				}
				
				if(file.isDirectory()) {
					contents = fine(file.getAbsolutePath(), contents);
				}
				
				if(file.toString().endsWith(".java")) {
					contents.add(file.getAbsolutePath());
				}
			}
			
		}
		
		return contents;
	}
	
}
