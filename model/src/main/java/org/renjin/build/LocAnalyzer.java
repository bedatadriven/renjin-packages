package org.renjin.build;

import com.google.common.collect.Maps;
import org.renjin.build.model.LOC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class LocAnalyzer {

	private final File baseDir;

	public LocAnalyzer(File baseDir) {
		this.baseDir = baseDir;
	}

	public LOC count() throws IOException {
		Map<String, Integer> map = countLoc();

		LOC loc = new LOC();
		if(map.containsKey("R")) {
			loc.setR(map.get("R"));
		}
		if(map.containsKey("Fortran")) {
			loc.setFortran(map.get("Fortran"));
		}
		if(map.containsKey("C")) {
			loc.setC(map.get("C"));
		}
		if(map.containsKey("C++")) {
			loc.setCpp(map.get("C++"));
		}
		return loc;
	}

	private Map<String, Integer> countLoc() throws IOException {
		System.out.println("Counting lines of code in " + this);
		Map<String, Integer> map = Maps.newHashMap();
		countLoc(map, "src");
		countLoc(map, "R");
		return map;
	}

	private void countLoc(Map<String, Integer> map, String subDir) throws IOException {
		File srcDir = new File(baseDir, subDir);
		if(srcDir.exists() && srcDir.listFiles() != null) {
			for(File srcFile : srcDir.listFiles()) {
				String lang = languageFromFile(srcFile);
				if(lang != null) {
					int loc = countLoc(srcFile);
					if(map.containsKey(lang)) {
						map.put(lang, map.get(lang) + loc);
					} else {
						map.put(lang, loc);
					}
				}
			}
		}

	}

	private int countLoc(File srcFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(srcFile));
		int lines = 0;
		while (reader.readLine() != null) lines++;
		reader.close();
		return lines;
	}

	private String languageFromFile(File srcFile) {
		String name = srcFile.getName().toLowerCase();
		if(name.endsWith(".cpp")) {
			return "C++";
		} else if(name.endsWith(".r") || name.endsWith(".s")) {
			return "R";
		} else if(name.endsWith(".c")) {
			return "C";
		} else if(name.endsWith(".f") || name.endsWith(".f77")) {
			return "Fortran";
		} else {
			return null;
		}
	}


}
