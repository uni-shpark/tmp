package com.shpark.m2m.cardinal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import com.shpark.m2m.helper.Commons;
import com.shpark.m2m.helper.history.CardinalHistoryManager;

@Component
public class CardinalDismantler {

	JSONArray cardinal;
	JSONArray rollback;
	JSONArray clean_classes;
	JSONObject modifying;
	String loc_msa_code;
	String loc_logs;
	String loc_org_src;
	Properties history_props;

	@Resource
	Commons commons;

	/**
	 * 
	 * @param inheritance 부모 / [자식] - JSONObject, JSONArray
	 * @param modifying   자식 / 변경할 부모 클래스명 - JSONObject, JSONObject
	 */

	public CardinalDismantler() {

	}

	private JSONArray rollback() {

		JSONArray resultlist = new JSONArray();
		
		for (int i = 0; i < rollback.size(); i++) {

			JSONObject result = new JSONObject();

			JSONObject bak_data = (JSONObject) rollback.get(i);
			String class_nm = "" + bak_data.get("class_nm");
			String partition = "commons";
			String org_partition = "" + this.history_props.getProperty(class_nm).split("&&")[1];
			int type = Integer.parseInt("" + bak_data.get("type"));
			resultlist = rollbackGeneration(class_nm, partition, org_partition, type, resultlist);

			rollbackLog(class_nm);
//			System.out.println("클래스 롤백 성공 : " + class_nm + " / 파티션 : " + partition);
			result.put("class_nm", class_nm);
			result.put("partition", org_partition);
			resultlist.add(result);

		}

		return resultlist;
	}

	private List<String> ignoreClasses() {

		List<String> list = new ArrayList<String>();

		Set set = this.history_props.keySet();
		Iterator it = set.iterator();

		while (it.hasNext()) {
			list.add("" + it.next());
		}

		return list;
	}

	public JSONArray cardinalSync(Properties history_props, String locCode, String locMsaCode, String locLogs) {

		this.history_props = history_props;
		this.loc_org_src = locCode;
		this.loc_msa_code = locMsaCode;
		this.loc_logs = locLogs;
		
		return cardinalSync();
	}

	private JSONArray cardinalSync() {

		Set set = this.history_props.keySet();
		Iterator it = set.iterator();

		JSONArray resultlist = new JSONArray();

		List<String> logs = syncLog();

		while (it.hasNext()) {

			String class_nm = "" + it.next();
			String bak_file = this.history_props.getProperty(class_nm).split(",")[0];
			String category = this.history_props.getProperty(class_nm).split("&&")[1];
			File org_file = getOrgLoc(class_nm);
			File new_file = getRenameLoc(class_nm);

//			cardinalCleanUp();
			classCopy(org_file, new_file);

			List<String> partitions = commons.getPartitionsFullName(class_nm);

			for (String partition : partitions) {
				File rename_target = new File(partition);
				File rename_nm = new File(rename_target + ".bak");
				rename_target.renameTo(rename_nm);
			}
			JSONObject result = new JSONObject();
			
			result.put("class_nm", class_nm);
			String bak_files = "";
			for (int i = 0; i < logs.size(); i++) {
				
				if(i>0) {
					bak_files = bak_files + "," + logs.get(i);
				} else {
					bak_files = ""+logs.get(i);
				}
			}
			result.put("bak_file", bak_files + "&&" + category);
			result.put("copy_partition", "commons");

			resultlist.add(result);
		}

		return resultlist;

	}

	private List<String> syncLog() {

		List<String> ignoreclasses = ignoreClasses();
//		String log_nm = this.history_props.getProperty(class_nm).split(",")[0];

		Set<Object> set = this.history_props.keySet();
		Iterator<Object> it = set.iterator();
		
		List<String> class_nm_list = new ArrayList<String>();
		List<String> result = new ArrayList<String>();
		while (it.hasNext()) {
			
			String class_nm = ""+it.next();
			class_nm_list.add(class_nm);
			
		}
		
		File loc = new File(this.loc_logs);

		File[] files = loc.listFiles();

		for (File file : files) {
			if (file.toString().endsWith("_cleaned"))
				continue;
			
			
			int index = logIndex(file.getName());
			
			BufferedReader br = null;

			try {

				File newfile = new File(file.toString() + "_" + (index + 1) + "_cleaned");
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				
				result.add(newfile.getName());
				StringBuffer file_buf = new StringBuffer();

				String line;
				int cnt = 0;
				while ((line = br.readLine()) != null) {
					boolean ignore_line = false;

					if (!(line.startsWith("|") && line.endsWith("|"))) {
						continue;
					}
					for (String ignore_class_nm : ignoreclasses) {
						if (line.indexOf(ignore_class_nm) > 0) {
							ignore_line = true;
							break;
						}
					}

					if (ignore_line)
						continue;

					cnt ++;
					file_buf.append(line + "\n");
					
					if(cnt > 1000) {
						filewrite(newfile, file_buf);
						cnt = 0;
						file_buf = new StringBuffer();
					}
				}
				br.close();

				filewrite(newfile, file_buf);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return result;

	}

	private int logIndex(String log_nm) {

		File loc_code = new File(this.loc_logs);
		String[] files = loc_code.list();

		Map<String, Integer> logIndex = new HashMap<String, Integer>();
		String org_log_nm = "";

		if (log_nm.endsWith("_cleaned")) {

			org_log_nm = log_nm.substring(0, log_nm.lastIndexOf("_cleaned"));
			org_log_nm = org_log_nm.substring(0, org_log_nm.indexOf("_"));

		} else {
			org_log_nm = log_nm;

		}

		for (String file_nm : files) {

			if (file_nm.indexOf(org_log_nm) >= 0) {
				if (logIndex.containsKey(org_log_nm)) {
					logIndex.put(org_log_nm, logIndex.get(org_log_nm) + 1);
				} else {
					logIndex.put(org_log_nm, 0);
				}
			}
		}
		return logIndex.get(org_log_nm);
	}

	private void rollbackLog(String class_nm) {

		String[] log_nms = this.history_props.getProperty(class_nm).split("&&")[0].split(",");
		String org_log_nm = "";

		for (String log_nm : log_nms) {
			if (log_nm.endsWith("_cleaned")) {

				org_log_nm = log_nm.substring(0, log_nm.lastIndexOf("_cleaned"));
				org_log_nm = org_log_nm.substring(0, org_log_nm.indexOf("_"));

			} else {
				org_log_nm = log_nm;
			}

			int cnt_cleaned_file = logIndex(org_log_nm);
			List<String> ignore_classes = ignoreClasses();
			ignore_classes.remove(class_nm);

			BufferedReader br = null;

			try {

				File file = new File(this.loc_logs + org_log_nm);
				File newfile = new File(this.loc_logs + org_log_nm + "_" + (cnt_cleaned_file + 1) + "_cleaned");
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

				StringBuffer file_buf = new StringBuffer();

				String line;
				int cnt = 0;
				while ((line = br.readLine()) != null) {
					boolean ignore_line = false;

					if (!(line.startsWith("|") && line.endsWith("|"))) {
						continue;
					}
					for (String ignore_class_nm : ignore_classes) {
						if (line.indexOf(ignore_class_nm) > 0) {
							ignore_line = true;
							break;
						}
					}

					if (ignore_line)
						continue;

					cnt ++;
					file_buf.append(line + "\n");
					
					if(cnt > 1000) {
						filewrite(newfile, file_buf);
						cnt = 0;
						file_buf = new StringBuffer();
					}
				}
				br.close();

				filewrite(newfile, file_buf);
				this.history_props.remove(class_nm);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private File getFileLoc(String full_nm, String partition) {

		String modify = full_nm.replace(".", "/") + ".java";
		File loc_code = new File(this.loc_msa_code + "src-" + partition + "/" + modify);

		return loc_code;

	}

	private File getCommonFileLoc(String full_nm, String partition) {

		String modify = full_nm.replace(".", "/") + ".java";
		File loc_code = new File(this.loc_msa_code + partition + "/" + modify);

		return loc_code;

	}

	private File getKluServiceFileLoc(String full_nm, String partition) {

		String modify = full_nm.replace(".", "/") + "Service.java";
		File loc_code = new File(this.loc_msa_code + "src-" + partition + "/" + modify);

		return loc_code;

	}

	private JSONArray rollbackGeneration(String class_nm, String current_partition, String org_partition, int type,
			JSONArray resultlist) {

		File loc_code = null;
		File loc_code_bak = null;

		JSONObject error = new JSONObject();

		if (type == 2) {
			loc_code = getKluServiceFileLoc(class_nm, org_partition);
			loc_code_bak = new File(loc_code.toString() + ".bak");

			if (loc_code_bak.exists()) {
				loc_code_bak.renameTo(loc_code);
				if (!loc_code.exists()) {
					error.put("error",
							"parentRollbackGeneration : KluService rollback failed : " + loc_code_bak.toString());
					resultlist.add(error);
				}
			}
		}
		for (String arg : commons.getPartitions()) {

			loc_code = getKluServiceFileLoc(class_nm, org_partition);
			loc_code_bak = new File(loc_code.toString() + ".bak");
			if (loc_code_bak.exists()) {
				loc_code_bak.renameTo(loc_code);
				if (!loc_code.exists()) {
					error.put("error",
							"parentRollbackGeneration : KluService rollback failed : " + loc_code_bak.toString());
					resultlist.add(error);
				}
			}

			loc_code = getFileLoc(class_nm, arg);
			loc_code_bak = new File(loc_code.toString() + ".bak");
			loc_code_bak.renameTo(loc_code);
			if (!loc_code.exists()) {
				error.put("error", "parentRollbackGeneration : class rollback failed : " + loc_code_bak.toString());
				resultlist.add(error);
			}
		}

		loc_code = getCommonFileLoc(class_nm, current_partition);
		if (!loc_code.exists()) {
			error.put("error", "parentRollbackGeneration : commons partition class not found : " + loc_code.toString());
			resultlist.add(error);
		} else {
			if (!loc_code.delete()) {
				error.put("error", "parentRollbackGeneration : class rollback failed : " + loc_code.toString());
				resultlist.add(error);
			}
		}

		return resultlist;

	}

	private boolean classCopy(File org_nm, File copy_nm) {

		BufferedReader br = null;

		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(org_nm), "UTF-8"));

			StringBuffer file = new StringBuffer();

			String line;
			while ((line = br.readLine()) != null) {
				file.append(line + "\n");
			}
			br.close();

			filewrite(copy_nm, file);

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

	private void filewrite(File file_loc, StringBuffer contents) throws IOException {

		BufferedWriter writer = null;
		File dir = file_loc.getParentFile();
		PrintWriter out = null;

		if (!dir.exists()) {
			dir.mkdirs();
		}
		try {

			FileWriter fw = new FileWriter(file_loc, true);
			BufferedWriter bw = new BufferedWriter(fw);
			out = new PrintWriter(bw);

			out.println(contents.toString());

//			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_loc), "UTF-8"));
//			writer.write(contents.toString());
//			writer.close();

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (writer != null)
					writer.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private JSONArray renameKlu(String class_nm, String partition, int type, JSONArray resultlist) {

		File loc_code = null;
		File kluService = null;

		JSONObject error = new JSONObject();
		if (type == 2) {
			loc_code = getKluServiceFileLoc(class_nm, partition);
			if (loc_code.exists()) {
				kluService = new File(loc_code.getAbsoluteFile() + ".bak");
				loc_code.renameTo(kluService);
				if (!kluService.exists()) {
					error.put("error", "renameKlu : KluService not found : " + loc_code.toString());
					resultlist.add(error);
				}
			}
		}
		for (String arg : commons.getPartitions()) {

			kluService = null;
			loc_code = getKluServiceFileLoc(class_nm, partition);

			if (loc_code.exists()) {
				kluService = new File(loc_code.getAbsoluteFile() + ".bak");
				loc_code.renameTo(kluService);
				if (!kluService.exists()) {
					error.put("error", "renameKlu : KluService not found : " + loc_code.toString());
					resultlist.add(error);
				}
			}

			loc_code = getFileLoc(class_nm, arg);
			kluService = new File(loc_code.getAbsoluteFile() + ".bak");
			loc_code.renameTo(kluService);
			if (!kluService.exists()) {
				error.put("error", "renameKlu : org class not found : " + loc_code.toString());
				resultlist.add(error);
			}
		}
		return resultlist;
	}

	public JSONArray rollback(Properties history_props, JSONArray rollback, String loc_code, String loc_logs) {

		this.history_props = history_props;
		this.rollback = rollback;
		this.loc_msa_code = loc_code;
		this.loc_logs = loc_logs;

		return rollback();
	}

	public JSONArray cardinalCleanUp(JSONArray args, String locLogs, String orgSrcDirNm, String msa_loc) {

		this.loc_logs = locLogs;
		this.loc_org_src = orgSrcDirNm;
		this.clean_classes = args;
		this.loc_msa_code = msa_loc;

		return cardinalCleanUp();
	}

	private File getOrgLoc(String class_nm) {

		String file_nm = class_nm.replace(".", "/") + ".java";
		String modify = this.loc_org_src + file_nm;
		File org_file = new File(modify);

		return org_file;
	}

	private File getRenameLoc(String class_nm) {

		String file_nm = class_nm.replace(".", "/") + ".java";
		String rename_str = this.loc_msa_code + "commons/" + file_nm;

		File rename = new File(rename_str);

		return rename;
	}

	private File getCommonLoc(String full_class_nm) {
		int idx = full_class_nm.lastIndexOf("/");
		if (idx < 0) {
			idx = full_class_nm.lastIndexOf("\\");
		}
		String common_dir = full_class_nm.substring(0, idx);

		File common = new File(common_dir);

		return common;
	}

	private JSONArray cardinalCleanUp() {

		JSONArray resultlist = new JSONArray();
		boolean copied = false;

		String class_nm = "" + ((JSONObject) this.clean_classes.get(0)).get("class_nm");
		String category = "" + ((JSONObject) this.clean_classes.get(0)).get("partition");
		String bak_file = cleanLog(class_nm);

		File loc_code = new File(this.loc_org_src);

		if (!loc_code.exists()) {
			System.out.println("org source directory isn't exist : " + this.loc_org_src);
			return null;
		}

		File org_file = getOrgLoc(class_nm);
		if (org_file.isDirectory() || !org_file.exists())
			return null;

		File rename = getRenameLoc(class_nm);

		File mkdirs = getCommonLoc(rename.getAbsolutePath());
		mkdirs.mkdirs();

		copied = classCopy(org_file, rename);

		JSONObject result = new JSONObject();
		result.put("class_nm", class_nm);
		result.put("bak_file", bak_file + "&&" + category);
		result.put("copy_partition", "commons");

		int type = Integer.parseInt("" + ((JSONObject) this.clean_classes.get(0)).get("type"));

		List<String> gen_partitions = (List<String>) commons.getPartitions();

		if (copied) {
			System.out.println("클래스 복제 성공 : " + rename);
			if (gen_partitions.contains(category))
				resultlist = renameKlu(class_nm, category, type, resultlist);
		} else {
			System.out.println("클래스 복제 실패 : " + rename.getAbsolutePath() + " 원본 위치 : " + org_file.getAbsolutePath());
			result.put("error", "class copy failed  : " + rename);
		}

		resultlist.add(result);

		return resultlist;

	}

	private String cleanLog(String class_nm) {

		File loc_code = new File(this.loc_logs);
		List<String> cleanLogList = new ArrayList<String>();
		if (!loc_code.exists()) {
			System.out.println("log directory isn't exist : " + this.loc_logs);
			return null;
		}

		String[] files = loc_code.list();

		boolean isExistCleaned = false;

		for (String file_nm : files) {

			int cnt_cleaned_file = logIndex(file_nm);

			if (cnt_cleaned_file > 0) {
				isExistCleaned = true;
			} 
			
			if(cnt_cleaned_file == 0 && !file_nm.endsWith("_cleaned"))
			{
				isExistCleaned = false;
			}

			if (isExistCleaned && !file_nm.endsWith("cleaned")) {
				continue;
			}

			File file = new File(loc_code + "/" + file_nm);
			if (file.isDirectory() || file.toString().indexOf("_cleaned") > 0) {
				if (!isExistCleaned) {
					continue;
				}
			}

			if (isExistCleaned && file.toString().indexOf(cnt_cleaned_file + "_cleaned") < 0)
				continue;

			String modify_file_nm = "";

			if (isExistCleaned) {
				modify_file_nm = file.toString().replace(cnt_cleaned_file + "_cleaned",
						(cnt_cleaned_file + 1) + "_cleaned");
			} else {
				modify_file_nm = file.toString() + "_" + (cnt_cleaned_file + 1) + "_cleaned";
			}
			File modify_file = new File(modify_file_nm);

			BufferedReader br = null;
			try {

				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

				StringBuffer log_contents = new StringBuffer();
				String line;

				int log_contents_cnt = 0;
				while ((line = br.readLine()) != null) {

					if (log_contents_cnt > 1000) {
						filewrite(modify_file, log_contents);
						log_contents_cnt = 0;
						log_contents = new StringBuffer();
					}

					if (!(line.startsWith("|") && line.endsWith("|"))) {
						continue;
					}
					if (line.indexOf(class_nm) >= 0) {
						continue;
					}

					log_contents.append(line + "\n");
					log_contents_cnt++;
				}

				filewrite(modify_file, log_contents);
				br.close();

				cleanLogList.add(file_nm);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String bak_file = "";

		for (int i = 0; i < cleanLogList.size(); i++) {

			if (i > 0) {
				bak_file = bak_file + "," + cleanLogList.get(i);
			} else {
				bak_file = cleanLogList.get(i);
			}
		}
		return bak_file;
	}
}
