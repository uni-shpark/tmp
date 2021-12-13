package com.shpark.m2m.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.shpark.m2m.constant.Constant;

@Component
public class Commons implements Constant{

	private static List<String> partitions;
	private static String flag = "";

	/**
	 * 
	 * @param inheritance 부모 / [자식] - JSONObject, JSONArray
	 * @param modifying   자식 / 변경할 부모 클래스명 - JSONObject, JSONObject
	 */

	static {
		if ("".equals(flag)) {
			synchronized (flag) {
				if (partitions == null) {

					partitions = new ArrayList<String>();
					File msa_loc = new File(loc_msa_code);

					File[] files = msa_loc.listFiles();

					for (File file : files) {
						if (!file.isDirectory())
							continue;

						if (file.toString().indexOf("partition") > 0)
							partitions.add(file.getName().replaceAll(loc_code + "-", ""));
					}
				}
				flag = "OK";
			}
		}
	}

	public List<String> getPartitions() {

		List<String> partitions = Commons.partitions;

		return partitions;

	}

	public List<String> getPartitionsFullName(String class_nm) {

		List<String> loc_full_dir = new ArrayList<String>();
		String loc_msa = System.getProperty("workspace") + "/msa/";
		String org_src_dir_nm = System.getProperty("org.src.dir.nm");

		String class_nm_1 = class_nm.replace(".", "/") + ".java";
		for (String partition : getPartitions()) {
			loc_full_dir.add(loc_msa + org_src_dir_nm + "-" + partition + "/" + class_nm_1);
		}

		String class_nm_2 = class_nm.replace(".", "/") + "Service.java";
		for (String partition : getPartitions()) {
			loc_full_dir.add(loc_msa + org_src_dir_nm + "-" + partition + "/" + class_nm_2);
		}

		return loc_full_dir;
	}

}
