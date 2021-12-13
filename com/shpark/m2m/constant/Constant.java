package com.shpark.m2m.constant;

import com.shpark.m2m.util.Configure;

public interface Constant {

	static String workspace = Configure.getProps().getProperty("workspace");
	static String org_src_dir_nm = Configure.getProps().getProperty("org.src.dir.nm");
	
	static String loc_code = workspace + "/" + org_src_dir_nm + "/";
	static String loc_msa_code = workspace + "/msa/";
	static String loc_final_graph = workspace + "/files/final_graph.json";
	static String loc_class_run = workspace + "/files/class_run.json";
	
	static String loc_org_final_graph = workspace + "/files/org/final_graph.json";
	static String loc_org_class_run = workspace + "/files/org/class_run.json";
	
	static String loc_symtable = workspace + "/files/symTable.json";
	static String loc_inheritance_history = workspace + "/history/history_inheritance.props";
	
	static String loc_cardinal_history = workspace + "/history/history_cardinal.props";
		
	static String loc_logs = workspace + "/logs/";
	
	static int server_port = Integer.parseInt(Configure.getProps().getProperty("socket.server.port"));
	
}
