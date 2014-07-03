package org.renjin.ci.model;


import com.google.common.collect.Lists;

import java.util.List;

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

  public List<Count> getCounts() {
    List<Count> counts = Lists.newArrayList();
    if(r > 0) {
      counts.add(new Count("R", r));
    }
    if(c > 0) {
      counts.add(new Count("C", c));
    }
    if(cpp > 0) {
      counts.add(new Count("C++", c));
    }
    if(fortran > 0) {
      counts.add(new Count("Fortran", fortran));
    }
    return counts;
  }

  public static class Count {
    private String language;
    private int lines;

    public Count(String language, int lines) {
      this.language = language;
      this.lines = lines;
    }

    public String getLanguage() {
      return language;
    }

    public int getLines() {
      return lines;
    }
  }
}
