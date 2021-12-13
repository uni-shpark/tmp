package com.shpark.m2m.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.shpark.m2m.constant.Constant;

public class Configure {
	
	private static Properties props = null;
	
	static {

		String loc_config = System.getProperty("uma.config");
		
		if (props == null) {

			BufferedReader br = null;
			System.out.println(loc_config);
			
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_config), "UTF-8"));
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
	
	public static Properties getProps() {
		Properties props = Configure.props;
		return props;
	}
}
