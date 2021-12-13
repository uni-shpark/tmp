package com.shpark.m2m.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class CodeView {

	private static File getFileLoc(String full_nm, String loc_code) {

		File loc = null;
		

		if (loc_code == null) {
			loc = new File(full_nm);
		} else {
			String modify = full_nm.replace(".", "/") + ".java";
			loc = new File(loc_code + "/" + modify);
		}
		return loc;

	}

	public static String load(String java_nm, String loc_code) {

		File loc_code_full = getFileLoc(java_nm, loc_code);

		BufferedReader br = null;
		BufferedWriter writer = null;
		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_code_full), "UTF-8"));

			StringBuffer file = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				file.append(line + "\n");
			}
			br.close();

			return file.toString();
		} catch (FileNotFoundException e) {
			return "FileNotFound ( " + java_nm + " )";
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
			}
			try {
				if (writer != null)
					writer.close();
			} catch (Exception e) {
			}
		}

		return null;
	}
}
