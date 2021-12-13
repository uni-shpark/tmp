package com.shpark.m2m.bci.server;

import static com.shpark.m2m.bci.server.helper.DataHelper.getTables;
import static com.shpark.m2m.bci.server.helper.DataHelper.del;
import static com.shpark.m2m.bci.server.helper.DataHelper.set;
import static com.shpark.m2m.bci.server.helper.DataHelper.unKnown;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.shpark.m2m.bci.server.lucene.Writer;
import com.shpark.m2m.constant.Constant;
import com.shpark.m2m.util.Configure;

public class BCIServer extends WebSocketServer implements Constant {

//	private static int server_port;
	private static InetSocketAddress inet;
	private static BCIServer server;

	static {

		try {

			inet = new InetSocketAddress(server_port);
			server = new BCIServer(inet);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static BCIServer getInstance() {
		BCIServer server = BCIServer.server;
		return server;
	}

	public BCIServer(InetSocketAddress inet) throws UnknownHostException {
		super(inet);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		conn.send("Welcome to the server!"); // This method sends a message to the new client
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn + " has left the room!");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {

		Map<String, Object> command = new HashMap<String, Object>();
		command.put("txid", message);
		Map<String, Object> luceneData = del(command);

		Writer.write(luceneData);

	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {

		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(message.array());
			ObjectInputStream ois = new ObjectInputStream(bis);
			@SuppressWarnings("unchecked")
			Map<String, Object> command = (Map<String, Object>) ois.readObject();

			String sql = ("" + command.get("sql_text")).toLowerCase();
			String stack_org = "" + command.get("stack");
			String[] stack_split = ("" + command.get("stack")).split("\n");

			StringBuffer full_stack = new StringBuffer();
			StringBuffer custom_stack = new StringBuffer();
			StringBuffer command_tables = new StringBuffer();
			
			String [] custom_package_prefixes = Configure.getProps().getProperty("custom.package.prefix").split(",");
			
			for (String line : stack_split) {
				int idx = line.indexOf("(");
				line = line.substring(0, idx).trim();
				full_stack.append(line + "\n");

				for (String prefix : custom_package_prefixes) {
					if (line.startsWith(prefix)) {
						String clazz_nm = line;
						if (clazz_nm.indexOf("$$") > 0) {
							clazz_nm = line.substring(0, line.indexOf("$$"));
						}

						if (custom_stack.toString().indexOf(clazz_nm) < 0)
							custom_stack.append(clazz_nm + "\n");
					}
				}
			}

			List<String> tables = getTables();

			for (String table : tables) {
				if (sql.indexOf(table) >= 0) {
//					System.out.println("table is : " + table);
					command_tables.append(table + "\n");
				}
			}

			if (command_tables.length() <= 0)
				return;

			if(sql.indexOf("select @{1} from dual") >=0 || sql.indexOf("select from dual") >= 0) {
				return;
			}
			
			String sql_type = null;
			
			if (sql.trim().startsWith("insert")) {
				sql_type = "[ C ]";
			} else if (sql.trim().startsWith("select") || sql.trim().startsWith("with") || sql.trim().startsWith("/*")) {
				sql_type = "[ R ]";
			} else if (sql.trim().startsWith("update")) {
				sql_type = "[ U ]";
			} else if (sql.trim().startsWith("delete")) {
				sql_type = "[ D ]";
			} else {
				sql_type = "unknown";
			}

			command.put("sql_type", sql_type);
			command.put("custom_stack", custom_stack.toString());
//			command.put("stack", full_stack.toString());
			command.put("stack_org", stack_org);
			command.put("tables", command_tables);

			String serviceName = "" + command.get("serviceName");

			if ("unknown".equals(serviceName))
				unKnown(command);

			set(command);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String hello() {
		this.start();
		return "BCI hello";
	}
}
