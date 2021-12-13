package com.shpark.m2m.inheritance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import com.shpark.m2m.helper.DataConversion;

@Component
public class InheritanceAnalyzer {

	String loc_symtable;
	String loc_code;

	public InheritanceAnalyzer() {
	}

	private JSONObject getInheritanceList() {

		JSONParser parser = new JSONParser();
		JSONObject jobj = null;
		JSONObject parents = null;

		try {

			File loc_symtable = new File(this.loc_symtable);
			BufferedReader br = new BufferedReader(new FileReader(loc_symtable));

			Object obj = parser.parse(br);

			if (obj instanceof JSONObject) {
				jobj = (JSONObject) obj;
				parents = getParents(jobj);
			}

			if (parents.size() == 0)
				return null;

			JSONObject inheritance = checkInheritance(parents);
			return validate(inheritance);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String getParentNmByJavaFileRead(String parent, String child, String child_java_nm) {

		File loc_code = new File(this.loc_code + "/" + child_java_nm);

		String parent_full_nm = null;

		try (BufferedReader br = new BufferedReader(new FileReader(loc_code))) {
			String line = null;
			StringBuffer code = new StringBuffer();

			boolean firstcheck = false;
			while ((line = br.readLine()) != null) {

				code.append(line + "\n");
				line = line.trim();
				if (line.startsWith("import") && line.indexOf(parent) > 0) { // 패키지명이 다른 경우
					parent_full_nm = (line.replace("import", "").replace(";", "")).trim();
					firstcheck = true;

				}
			}

			if (!firstcheck && code.toString().indexOf(parent) < 0) {
				if (code.toString().indexOf(parent) < 0) { // M2M의 symTable.json과 데이터가 다를때. 누군가 변경하거나, M2M 툴이 변경함.
					return getParentNmByJavaFileRead(parent, child, child.replace(".", "/") + ".java.bak");
				}
			}
			if (parent_full_nm == null) { // 패키지명이 동일한 경우
				parent_full_nm = child.substring(0, child.lastIndexOf(".")) + "." + parent;
			}

//		    System.out.println(child + " / " + parent_full_nm);

		} catch (IOException e) {
			e.printStackTrace();

			return null;
		}

		return parent_full_nm;
	}

	private JSONObject validate(JSONObject inheritance) {

		JSONObject check = new JSONObject();
		JSONObject last_check = new JSONObject();

		Set<String> set = inheritance.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String parent = it.next();
			JSONArray children = (JSONArray) inheritance.get(parent);

			String inner_nm = null;
			// 자식 JAVA 파일에서 부모클래스 정보를 확인
			for (int i = 0; i < children.size(); i++) {
				String child = "" + children.get(i);
				String parent_full_nm = "";

				if (child.indexOf("$") > 0) {

					int first_index = child.indexOf("$");

					String new_nm = child.substring(0, first_index - 1);
					inner_nm = child.substring(first_index + 1);
					parent_full_nm = getParentNmByJavaFileRead(parent, new_nm, new_nm.replace(".", "/") + ".java");
					System.out.println(new_nm);
				} else {
					parent_full_nm = getParentNmByJavaFileRead(parent, child, child.replace(".", "/") + ".java");
				}
				// 확인된 부모클래스 정보(패키지명)를 저장
				check = compare(last_check, parent_full_nm, child);

			}
		}

		last_check = checkInheritance(check);
		return last_check;
	}

	private JSONObject checkInheritance(JSONObject parents) {

		JSONObject inheritance = new JSONObject();

		Set<String> set = parents.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String parent_class_nm = it.next();
			JSONArray children = (JSONArray) parents.get(parent_class_nm);

			if(children.size() <= 1)
				continue;

			inheritance.put(parent_class_nm, (JSONArray) parents.get(parent_class_nm));

		}
		return inheritance;

	}

	private JSONObject getParents(JSONObject jobj) {

		JSONObject parents = new JSONObject();

		Set<String> set = jobj.keySet();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {

			String key = it.next();
			JSONObject root = (JSONObject) jobj.get(key);

			JSONObject super_obj = (JSONObject) root.get("super");
			if (super_obj == null) {
				continue;
			}
//			System.out.println(root);
			JSONObject extends_obj = (JSONObject) super_obj.get("extends");

			if (extends_obj == null) {
				continue;
			}

			JSONArray super_class_list = (JSONArray) extends_obj.get("values");

			String super_class_nm = "" + super_class_list.get(0);
			String class_nm = "" + root.get("FQCN");

			if (class_nm.indexOf("$") > 0) {
				System.out.println();
			}

			if ("HttpServlet".equals(super_class_nm) || "RuntimeException".equals(super_class_nm)
					|| "ContextLoader".equals(super_class_nm))
				continue;

			parents = compare(parents, super_class_nm, class_nm);

		}

//		System.out.println(parents);
		return parents;

	}

	private JSONObject compare(JSONObject parents, String super_class_nm, String class_nm) {

		if (parents.containsKey(super_class_nm)) {
			JSONArray children = (JSONArray) parents.get(super_class_nm);
			children.add(class_nm);
			parents.put(super_class_nm, children);
		} else {
			JSONArray children = new JSONArray();
			children.add(class_nm);
			parents.put(super_class_nm, children);
		}

		return parents;
	}

	public JSONObject getInheritanceList(String loc_symtable, String loc_code) {
		this.loc_symtable = loc_symtable;
		this.loc_code = loc_code;

		return getInheritanceList();
	}
}
