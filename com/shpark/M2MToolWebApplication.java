package com.shpark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.shpark.m2m.bci.server.BCIServer;
import com.shpark.m2m.bci.server.lucene.HashTables;
import com.shpark.m2m.util.Configure;

@SpringBootApplication(scanBasePackages = {"com.shpark"})
public class M2MToolWebApplication {
	
	public static void main(String[] args) {
		
//		String class_nm = "DUAL, ";
//		String tmp = "DUAL";
//		String package_nm = class_nm.substring(0, class_nm.lastIndexOf(","));
//		System.out.println(package_nm);
//
//		if (package_nm.equals(tmp)){
//			System.out.println("!!!");
//		} else {
//			System.out.println("123123");
//		}
		SpringApplication.run(M2MToolWebApplication.class, args);
		System.out.println(BCIServer.getInstance().hello());
		System.out.println(HashTables.hello());
		System.out.println(Configure.getProps().toString());
	}
}
