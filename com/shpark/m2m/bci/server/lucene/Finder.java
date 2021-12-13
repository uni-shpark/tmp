package com.shpark.m2m.bci.server.lucene;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.shpark.m2m.cardinal.CardinalAnalyzer;
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.table.TableAnalyzer;
import com.shpark.m2m.util.Configure;

public class Finder implements Constant {

	static String flag = "";

	private static IndexSearcher init() {

		IndexSearcher finder = null;

		if (finder == null) {
			File fileIndex = new File(workspace + "/index");
			try {
				Directory dir = FSDirectory.open(Paths.get(fileIndex.toURI()));
				IndexReader reader = DirectoryReader.open(dir);
				finder = new IndexSearcher(reader);
			} catch (Exception e) {
//					e.printStackTrace();
			}
		}
		return finder;
	}

	public static List<Document> findHardly(String stack_str) {

		Document doc = null;
		List<Document> docList = new ArrayList<Document>();
		try {

			Query wordQuery = new QueryBuilder(new StandardAnalyzer()).createBooleanQuery("custom_stacks", stack_str,
					Occur.MUST);

			IndexSearcher finder = init();

			if (finder == null) {
				init();
				if (finder == null)
					return docList;
			}

			TopDocs foundDocsBody = finder.search(wordQuery, 1000);

			System.out.println("total count : " + foundDocsBody.totalHits);
			for (ScoreDoc sd : foundDocsBody.scoreDocs) {
				doc = finder.doc(sd.doc);
				docList.add(doc);
//				System.out.println(doc.get("serviceName"));
//				System.out.println(doc.get("tables"));
//				System.out.println(doc.get("custom_stacks"));
//				System.out.println(doc.get("sqls"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return docList;
	}

	private static String setCRUD(String[] sqltext) {

		for (String line : sqltext) {
			line = line.toLowerCase();

			if (line.trim().startsWith("insert")) {
				return "[ C ]";
			} else if (line.trim().startsWith("select") || line.trim().startsWith("with")
					|| line.trim().startsWith("/*")) {
				return "[ R ]";
			} else if (line.trim().startsWith("update")) {
				return "[ U ]";
			} else if (line.trim().startsWith("delete")) {
				return "[ D ]";
			}
		}

		return "unknown";
	}

	public static void findSoftlyWithJArraySimple(String class_full_nm, String partition, JSONArray merged) {

		List<Document> docList = findSoftly(class_full_nm);
		int idx = 0;
		for (Document doc : docList) {
			JSONObject unit = new JSONObject();
			String custom_stck = doc.get("custom_stacks");

			if (custom_stck.indexOf(class_full_nm) < 0)
				continue;

			String tables = (doc.get("tables")).replaceAll("\n", ", ").toUpperCase();
			int lastIdx = tables.lastIndexOf(",");
			if (lastIdx >= 1) {
				tables = tables.substring(0, tables.lastIndexOf(","));
			}
			
			if(tables.indexOf("DUAL") >= 0) {
				tables = tables.replace("DUAL, ", "");
				tables = tables.replace(", DUAL", "");
				tables = tables.replace("DUAL", "");
			}
			if ("".equals(tables.trim())) {
				continue;
			}
			String sql_type = (doc.get("sql_type"));

			if ("unknown".equals(sql_type)) {
				sql_type = setCRUD(doc.get("sqls").split("\n"));
			}
			String class_nm = class_full_nm.substring(class_full_nm.lastIndexOf(".") + 1, class_full_nm.length());
			// class_full_nm
			// tables
			// stack
			// sql
			// serviceName
			System.out.println("hit class name : " + class_nm);
			unit.put("serviceName", doc.get("serviceName"));
			unit.put("tables", tables);
			unit.put("class_nm", class_nm);
			unit.put("class_full_nm", class_full_nm);
			unit.put("sql_type", sql_type);
			unit.put("partition", partition);
			unit.put("idx", idx);
//			unit.put("stack", doc.get("stack"));
//			unit.put("sqls", doc.get("sqls"));

			merged.add(unit);

			idx++;
		}
	}

	public static JSONObject findSoftlyWithJArraySimple(JSONObject data) {

		String class_full_nm = "" + data.get("class_full_nm");
		String tables = "" + data.get("tables");
		String serviceName = "" + data.get("serviceName");
		int idx = Integer.parseInt("" + data.get("idx"));
		List<Document> docList = findSoftly(class_full_nm);

		JSONObject result = new JSONObject();

		String[] custom_package_prefixes = Configure.getProps().getProperty("custom.package.prefix").split(",");

		int idx_cnt = 0;
		for (Document doc : docList) {

			if (idx_cnt == idx) {
				JSONObject unit = new JSONObject();
				String doc_tables = (doc.get("tables")).replaceAll("\n", ", ").toUpperCase();
				doc_tables = doc_tables.substring(0, doc_tables.lastIndexOf(","));
				String doc_serviceName = doc.get("serviceName");

				if (!(tables.equals(doc_tables) && serviceName.equals(doc_serviceName)))
					continue;

				String stack_org = doc.get("stack_org");
				String[] custom_stack_org = stack_org.split("\n");
				StringBuffer sb = new StringBuffer();
				for (String stack : custom_stack_org) {

					for (String prefix : custom_package_prefixes) {
						prefix = prefix.trim();
						if (stack.startsWith(prefix))
							sb.append(stack + "\n");
					}

				}
				result.put("sql", doc.get("sqls"));
				result.put("stack", sb.toString() + "\n\n\n=================================\n\n\n" + stack_org);

				return result;
			} else {
				idx_cnt++;
			}
		}

		return null;
	}

	public static List<Document> findSoftly(String stack_str) {

		Document doc = null;
		List<Document> docList = new ArrayList<Document>();

		IndexSearcher finder = init();

		try {

			Query wordQuery = new QueryBuilder(new StandardAnalyzer()).createBooleanQuery("custom_stacks", stack_str,
					Occur.SHOULD);

			if (finder == null) {
				init();
				if (finder == null)
					return docList;
			}

			TopDocs foundDocsBody = finder.search(wordQuery, 1000);

//			System.out.println("total count : " + foundDocsBody.totalHits);
			for (ScoreDoc sd : foundDocsBody.scoreDocs) {

				doc = finder.doc(sd.doc);
				docList.add(doc);

//				System.out.println("=======================");
//				System.out.println(doc.get("serviceName"));
//				System.out.println(doc.get("tables"));
//				System.out.println(doc.get("custom_stacks"));
//				System.out.println(doc.get("sqls"));

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return docList;
	}

	public static List<Document> findAll() {

		Document doc = null;
		List<Document> docList = new ArrayList<Document>();

		IndexSearcher finder = init();

		try {

			Query wordQuery = new WildcardQuery(new Term("custom_stacks", "*"));

			if (finder == null) {
				init();
				if (finder == null)
					return docList;
			}

			TopDocs foundDocsBody = finder.search(wordQuery, 100000);

			System.out.println("total count : " + foundDocsBody.totalHits);
			for (ScoreDoc sd : foundDocsBody.scoreDocs) {

				doc = finder.doc(sd.doc);
				docList.add(doc);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return docList;
	}

	public static void main(String[] args) {
//		StringBuffer findHardly = new StringBuffer();
//		findHardly.append("com.uni.sellers.datasource.AbstractDAO\n");
//		findHardly.append("com.uni.sellers.clientsatisfaction.ClientSatisfactionDAO\n");
//		findHardly.append("com.uni.sellers.logger.ProceedAdvice\n");
//		findHardly.append("com.uni.sellers.logger.LoggerAspect\n");
//		findHardly.append("com.uni.sellers.clientsatisfaction.ClientSatisfactionService\n");
//		findHardly.append("com.uni.sellers.clientsatisfaction.ClientSatisfactionController\n");
//		findHardly.append("com.uni.sellers.restful.SimpleCorsFilter\n");
//		findHardly(findHardly.toString());

		JSONArray merged = new JSONArray();
		StringBuffer findSoftly = new StringBuffer();
//		findSoftly.append("com.uni.sellers.datasource.AbstractDAO\n");
		findSoftly.append("OUCMCM902S0\n");
		TableAnalyzer ca = new TableAnalyzer();

		JSONArray resultlist = new JSONArray();

		resultlist = ca.getTableList(loc_final_graph, resultlist, null);

		System.out.println(resultlist.toJSONString());
	}
}
