package com.shpark.m2m.inheritance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class InheritanceDismantler {

	JSONObject inheritance;
	JSONObject modifying;
	String loc_code;
	Properties history_props;

	/**
	 * 
	 * @param inheritance 부모 / [자식] - JSONObject, JSONArray
	 * @param modifying   자식 / 변경할 부모 클래스명 - JSONObject, JSONObject
	 */

	public InheritanceDismantler() {

	}

	private JSONArray rollback() {

		JSONArray children;
		JSONArray resultlist = new JSONArray();

		boolean isSuccess = false;

		String parent_nm = "" + inheritance.get("parent");
		children = (JSONArray) inheritance.get("children");

		for (int i = 0; i < children.size(); i++) {

			JSONObject result = new JSONObject();

			String child_nm = "" + children.get(i);
			String history_key = parent_nm + "@" + child_nm;
			String new_parent_nm = history_props.getProperty(history_key);

			isSuccess = parentRollbackGeneration(new_parent_nm, child_nm);

			if (isSuccess) {

				System.out.println("클래스 롤백 성공 : " + child_nm);
				result.put("parent_nm", parent_nm);
				result.put("child_nm", child_nm);
				resultlist.add(result);

			} else {
				result.put("error", "class rollback failed : " + new_parent_nm + " / " + child_nm);
				resultlist.add(result);
			}
		}

		return resultlist;
	}

	private JSONArray dismantle() {

		JSONArray children;
		JSONArray resultlist = new JSONArray();

		String parent_nm = "" + inheritance.get("parent");
		children = (JSONArray) inheritance.get("children");

		for (int i = 0; i < children.size(); i++) {

			boolean occursErr = false;
			String child_nm = "" + children.get(i);
			String new_parent_nm = getModifyingNm(parent_nm, child_nm);

			boolean generated = parentClassGeneration(new_parent_nm, parent_nm);

			JSONObject result = new JSONObject();
			if (generated) {
				System.out.println("부모 클래스 변환 성공 : " + new_parent_nm + " 원본 코드 : " + parent_nm);

				boolean childClassGeneration = childClassGeneration(new_parent_nm, parent_nm, child_nm);

				if (childClassGeneration) {
					System.out.println("자식 클래스 변환 성공 : " + child_nm);
					result.put("new_parent_nm", new_parent_nm);
					result.put("parent_nm", parent_nm);
					result.put("child_nm", child_nm);
					resultlist.add(result);
				} else {
					System.out.println("자식 클래스 변환 실패 : " + child_nm);
					occursErr = true;
				}
			} else {
				System.out.println("부모 클래스 변환 실패 : " + new_parent_nm + " 원본 코드 : " + parent_nm);
				occursErr = true;
			}

			if (occursErr) {
				if (new_parent_nm.equals(history_props.get(parent_nm + "@" + child_nm))) {
					result.put("new_parent_nm", new_parent_nm);
					result.put("parent_nm", parent_nm);
					result.put("child_nm", child_nm);
					resultlist.add(result);
					continue;
				}
				result.put("error", "class generate failed  : " + parent_nm + " / " + child_nm);
				resultlist.add(result);
			}
		}
		return resultlist;
	}

	private File getFileLoc(String full_nm) {

		String modify = full_nm.replace(".", "/") + ".java";
		File loc_code = new File(this.loc_code + "/" + modify);

		return loc_code;

	}

	private boolean parentRollbackGeneration(String new_parent_full_nm, String child_full_nm) {

		File loc_new_parent = getFileLoc(new_parent_full_nm);
		String loc_child_bak_str = getFileLoc(child_full_nm).toString() + ".bak";
		File loc_child_bak = new File(loc_child_bak_str);
		File loc_child = getFileLoc(child_full_nm);

		boolean isSuccess = false;
		if (loc_new_parent.exists() && loc_child_bak.exists()) {
			isSuccess = true;

			loc_new_parent.delete();
			loc_child.delete();
			loc_child_bak.renameTo(new File(loc_child.toString()));
		}

		return isSuccess;

	}

	private boolean parentClassGeneration(String new_full_nm, String parent_full_nm) {

		File loc_code = getFileLoc(parent_full_nm);
		BufferedReader br = null;
		try {

			String parent_nm = parent_full_nm.substring(parent_full_nm.lastIndexOf(".") + 1);
			String new_nm = new_full_nm.substring(new_full_nm.lastIndexOf(".") + 1);

			br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_code), "UTF-8"));

			StringBuffer file = new StringBuffer();

			String line;
			while ((line = br.readLine()) != null) {
				
				String tmp = line;
				
				String [] tmps = tmp.replace("{","").split(" ");
				String[] lines = line.split(" ");
				
				if (line.indexOf("class") > 0 && line.indexOf(".class") < 0 && line.indexOf(parent_nm) > 0) {

					boolean isClassName = false;
					StringBuffer line_bf = new StringBuffer();
					for (String value : lines) {

						if ("class".equals(value.trim())) {
							isClassName = true;
							line_bf.append(value + " ");
							continue;
						}

						if (isClassName) {
							value = value.replace(parent_nm, new_nm);
							isClassName = false;
						}
						line_bf.append(value + " ");

					}
					line = line_bf.toString();
				} else if ((line.indexOf("public") > 0 || line.indexOf("private")  > 0|| line.indexOf("protected")  > 0) && tmps.length >= 3 && line.indexOf(".class") < 0) {
					file.append(line + "\n");
					continue;
				} 
				else if ( !(line.indexOf("public") > 0 || line.indexOf("private")  > 0|| line.indexOf("protected")  > 0) && tmps.length >= 2 && line.indexOf(".class") < 0) {
					file.append(line + "\n");
					continue;
				}
				else if (line.indexOf("protected") > 0 && line.indexOf("class") < 0 && line.indexOf(parent_nm) > 0
						&& line.indexOf("()") > 0) {
					line = line.replace(parent_nm, new_nm);
				} else if (line.indexOf("public") > 0 && line.indexOf("class") < 0 && line.indexOf(parent_nm) > 0
						&& line.indexOf("()") > 0) {
					line = line.replace(parent_nm, new_nm);
				} else if (line.indexOf("private") > 0 && line.indexOf("class") < 0 && line.indexOf(parent_nm) > 0
						&& line.indexOf("()") > 0) {
					line = line.replace(parent_nm, new_nm);
				} else if (line.indexOf("class") < 0 && line.indexOf(parent_nm) > 0 && line.indexOf("()") > 0) {
					line = line.replace(parent_nm, new_nm);
				} else if (line.indexOf(parent_nm+".class") > 0) {
					line = line.replace(parent_nm, new_nm);
				}
				file.append(line + "\n");
			}

			br.close();

			File loc_write_code = getFileLoc(new_full_nm);

			filewrite(loc_write_code.toString(), file);

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
			}
		}
	}

	private void filewrite(String file_loc, StringBuffer contents) throws IOException {

		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_loc), "UTF-8"));
			writer.write(contents.toString());
			writer.close();
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private boolean childClassGeneration(String new_full_nm, String parent_full_nm, String child) {

		File loc_code = getFileLoc(child);

		BufferedReader br = null;

		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(loc_code), "UTF-8"));

			String parent_nm = parent_full_nm.substring(parent_full_nm.lastIndexOf(".") + 1);
			String new_nm = new_full_nm.substring(new_full_nm.lastIndexOf(".") + 1);

			StringBuffer new_file = new StringBuffer();
			StringBuffer bak_file = new StringBuffer();
			String line;

			boolean firstcheck = false;
			while ((line = br.readLine()) != null) {

				bak_file.append(line + "\n");
				if (line.trim().startsWith("import") && line.indexOf(parent_full_nm) > 0) {
					line = line.replace(parent_full_nm, new_full_nm);
				}

				else if (line.indexOf("extends") > 0 && line.indexOf(parent_nm) > 0) {
					line = line.replace(parent_nm, new_nm);
				}
				new_file.append(line + "\n");
			}

			if (bak_file.indexOf(parent_nm) < 0) {
				return false;
			}
			br.close();

			filewrite(loc_code + ".bak", bak_file);
			filewrite(loc_code.toString(), new_file);

			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
			}
		}

		return false;
	}

	public String getModifyingNm(String parent_full_nm, String child) {

		if (modifying != null && modifying.get(child) != null)
			return "" + modifying.get(child);

		String new_class_nm = "Klu" + child.substring(child.lastIndexOf(".") + 1);
		String new_full_nm = parent_full_nm.substring(0, parent_full_nm.lastIndexOf(".")) + "." + new_class_nm;

		return new_full_nm;

	}

	public JSONArray dismantle(Properties history_props, JSONObject inheritance, JSONObject object, String loc_code) {

		this.history_props = history_props;
		this.inheritance = inheritance;
		this.modifying = object;
		this.loc_code = loc_code;

		return dismantle();
	}

	public JSONArray rollback(Properties history_props, JSONObject inheritance, String loc_code) {

		this.history_props = history_props;
		this.inheritance = inheritance;
		this.loc_code = loc_code;

		return rollback();
	}

}
