package com.shpark.m2m.bci.server.lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import com.shpark.m2m.constant.Constant;

public class Writer implements Constant {

	static IndexWriter writer = null;
	static String flag = "";
	static {

		File fileIndex = new File(workspace + "/index");
		FSDirectory dir;

		try {
			dir = FSDirectory.open(Paths.get(fileIndex.toURI()));

			IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());

			synchronized (flag) {

				if (writer == null) {
					writer = new IndexWriter(dir, config);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(Map<String, Object> luceneData) {

//		List<String> stacklist = (ArrayList<String>) luceneData.get("stacklist");
		List<String> stacklist_org = (ArrayList<String>) luceneData.get("stacklist_org");
		List<String> custom_stacklist = (ArrayList<String>) luceneData.get("custom_stacklist");
		List<String> sqllist = (ArrayList<String>) luceneData.get("sqllist");
		List<String> sql_type_list = (ArrayList<String>) luceneData.get("sql_type_list");
		List<String> tablelist = (ArrayList<String>) luceneData.get("tablelist");
		String serviceName = "" + luceneData.get("serviceName");
		Document doc = null;

		try {

			for (int i = 0; i < sqllist.size(); i++) {
				String sql = sqllist.get(i);
//				String stack = stacklist.get(i);
				String stack_org = stacklist_org.get(i);
				String custom_stack = custom_stacklist.get(i);
				String tables = tablelist.get(i);
				String sql_type = sql_type_list.get(i);
				System.out.println(custom_stack);
//				List<Document> doclist = Finder.findHardly(custom_stack);
//
//				boolean isDup = false;
//				if (doclist.size() > 0) {
//					for (Document stored_doc : doclist) {
//
//						String stored_custom_stack = stored_doc.get("custom_stacks");
//						String stored_sql = stored_doc.get("sqls");
//
//						if (stored_custom_stack.equals(custom_stack) && stored_sql.equals(sql)) {
//							isDup = true;
//							break;
//						}
//					}
//				}
				
				if(!HashTables.dup(custom_stack, sql)){
					continue;
				}
				
				doc = new Document(); // 1 row
				doc.add(new TextField("serviceName", serviceName, Store.YES));
				doc.add(new TextField("sqls", sql, Store.YES));
				doc.add(new TextField("tables", tables, Store.YES));
				doc.add(new TextField("custom_stacks", custom_stack, Store.YES));
//				doc.add(new TextField("stack", stack, Store.YES));
				doc.add(new TextField("stack_org", stack_org, Store.YES));
				doc.add(new TextField("sql_type", sql_type, Store.YES));

				writer.addDocument(doc);
				writer.commit();
				writer.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
