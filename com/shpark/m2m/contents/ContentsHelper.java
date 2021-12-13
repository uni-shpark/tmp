package com.shpark.m2m.contents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ContentsHelper {

	private File getFileLoc(String full_nm, String partition) {

		File loc_code = null;
		if(partition == null) {
//			loc_code = new File(this.loc_msa_code + "-" + partition + "/" + modify);
		}
		return loc_code;

	}
	
	private boolean classCopy(String class_nm, String partition) {

		File readFile = null;
		File copyFile = null;

		copyFile = getFileLoc(class_nm, partition);

		BufferedReader br = null;

		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(readFile), "UTF-8"));

			StringBuffer file = new StringBuffer();

			String line;
			while ((line = br.readLine()) != null) {
				file.append(line + "\n");
			}
			br.close();

//			filewrite(copyFile, file);

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

}
