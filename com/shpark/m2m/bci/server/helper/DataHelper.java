package com.shpark.m2m.bci.server.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.shpark.m2m.bci.server.lucene.Writer;
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.util.Configure;

public class DataHelper implements Constant {

	private static List<String> tables = null;
	private static Map<String, String> dao_list = null;
	private static String flag1 = "";
	private static String flag2 = "";

	private static Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();

	private static Map<String, Object> access(Map<String, Object> command, int type) {

//		System.out.println(command.get("txid"));
//		System.out.println(command.get("stacklist")); // LIST
//		System.out.println(command.get("serviceName"));
//		System.out.println(command.get("sqllist")); // LIST
//		System.out.println(command.get("tables")); // LIST

		Map<String, Object> luceneData = null;

		synchronized (data) {

			String txid = "" + command.get("txid");

			switch (type) {
			case 1:

				Map<String, Object> profiles = new HashMap<String, Object>();

				if (data.containsKey(txid)) {
					profiles = data.get(txid);
//					List<String> stacklist = (List<String>) profiles.get("stacklist");
					List<String> stacklist_org = (List<String>) profiles.get("stacklist_org");
					List<String> custom_stacklist = (List<String>) profiles.get("custom_stacklist");
					List<String> sqllist = (List<String>) profiles.get("sqllist");
					List<String> tablelist = (List<String>) profiles.get("tablelist");
					List<String> sql_type_list = (List<String>) profiles.get("sql_type_list");

					String stack_org = "" + command.get("stack_org");
					String custom_stack = "" + command.get("custom_stack");
					String sql_text = "" + command.get("sql_text");
					String tables = "" + command.get("tables");
					String sql_type = "" + command.get("sql_type");

					if (!sqllist.contains(sql_text) && !custom_stacklist.contains(custom_stack)) {
						sqllist.add(sql_text);
						sql_type_list.add(sql_type);
//						stacklist.add(stack);
						stacklist_org.add(stack_org);
						tablelist.add(tables);
						custom_stacklist.add(custom_stack);

//						profiles.put("stacklist", stacklist);
						profiles.put("stacklist_org", stacklist_org);
						profiles.put("custom_stacklist", custom_stacklist);
						profiles.put("sqllist", sqllist);
						profiles.put("tablelist", tablelist);

						data.put(txid, profiles);
					}

				} else {

					profiles = new HashMap<String, Object>();

//					List<String> stacklist = new ArrayList<String>();
//					stacklist.add("" + command.get("stack"));

					List<String> sql_type_list = new ArrayList<String>();
					sql_type_list.add("" + command.get("sql_type"));

					List<String> stacklist_org = new ArrayList<String>();
					stacklist_org.add("" + command.get("stack_org"));

					List<String> custom_stacklist = new ArrayList<String>();
					custom_stacklist.add("" + command.get("custom_stack"));

					List<String> sqllist = new ArrayList<String>();
					sqllist.add("" + command.get("sql_text"));

					List<String> tablelist = new ArrayList<String>();
					tablelist.add("" + command.get("tables"));

//					profiles.put("stacklist", stacklist);
					profiles.put("sql_type_list", sql_type_list);
					profiles.put("custom_stacklist", custom_stacklist);
					profiles.put("sqllist", sqllist);
					profiles.put("stacklist_org", stacklist_org);
					profiles.put("tablelist", tablelist);
					profiles.put("serviceName", "" + command.get("serviceName"));
					profiles.put("txid", txid);

					data.put(txid, profiles);

				}
				break;
			case 2:
				System.out.println("delete txid : " + txid + ", current data size : " + data.size());
				luceneData = new HashMap<String, Object>(data.get(txid));
				data.remove(txid);
				System.out.println(
						"deleted txid : " + txid + ", current data size atfer to delete txid : " + data.size());

				return luceneData;

			case 3:
				profiles = new HashMap<String, Object>();

//				List<String> stacklist = new ArrayList<String>();
//				stacklist.add("" + command.get("stack"));

				List<String> sql_type_list = new ArrayList<String>();
				sql_type_list.add("" + command.get("sql_type"));

				List<String> stacklist_org = new ArrayList<String>();
				stacklist_org.add("" + command.get("stack_org"));

				List<String> custom_stacklist = new ArrayList<String>();
				custom_stacklist.add("" + command.get("custom_stack"));

				List<String> sqllist = new ArrayList<String>();
				sqllist.add("" + command.get("sql_text"));

				List<String> tablelist = new ArrayList<String>();
				tablelist.add("" + command.get("tables"));

//				profiles.put("stacklist", stacklist);
				profiles.put("sql_type_list", sql_type_list);
				profiles.put("custom_stacklist", custom_stacklist);
				profiles.put("sqllist", sqllist);
				profiles.put("stacklist_org", stacklist_org);
				profiles.put("tablelist", tablelist);
				profiles.put("serviceName", "" + command.get("serviceName"));
				
				Writer.write(profiles); 
				break;
			}
		}
		return null;
	}

	public static void set(Map<String, Object> command) {
		access(command, 1);
	}

	public static Map<String, Object> del(Map<String, Object> command) {
		return access(command, 2);
	}

	public static void unKnown(Map<String, Object> command) {
		access(command, 3);
	}

	public static List<String> getTables() {

		Properties props = Configure.getProps();
		if (tables != null) {
			return tables;
		} else {

			synchronized (flag1) {

				if (tables != null)
					return tables;

				try {
					tables = new ArrayList<String>();
					String[] table_list = props.get("table_list").toString().split(",");

					for (String table_nm : table_list) {
						tables.add(table_nm.trim());
					}
					return tables;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static Map<String, String> getDaoList() {

		Properties props = Configure.getProps();
		if (dao_list != null) {
			return dao_list;
		} else {

			synchronized (flag2) {

				if (dao_list != null)
					return dao_list;

				try {
					dao_list = new HashMap<String, String>();
					String ignore = props.get("ignore_dao_class").toString();
					String accept = props.get("accept_dao_class").toString();

					dao_list.put("ignore_dao_class", ignore);
					dao_list.put("accept_dao_class", accept);

					return dao_list;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
