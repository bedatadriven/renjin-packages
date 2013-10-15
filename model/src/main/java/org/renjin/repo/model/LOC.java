package org.renjin.repo.model;


import com.google.common.collect.Maps;

import java.util.Map;

public class LOC {
	private int r;
	private int c;
	private int fortran;
	private int cpp;

	public int getR() {
		return r;
	}

	public void setR(int r) {
		this.r = r;
	}

	public int getC() {
		return c;
	}

	public void setC(int c) {
		this.c = c;
	}

	public int getFortran() {
		return fortran;
	}

	public void setFortran(int fortran) {
		this.fortran = fortran;
	}

	public int getCpp() {
		return cpp;
	}

	public void setCpp(int cpp) {
		this.cpp = cpp;
	}
  
  public Map<String, Integer> asMap() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("R", r);
    map.put("C", c);
    map.put("C++", cpp);
    map.put("Fortran", fortran);
    return map;
  }
}
