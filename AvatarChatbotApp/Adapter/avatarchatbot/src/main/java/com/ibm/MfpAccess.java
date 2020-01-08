package com.ibm;

public class MfpAccess {
    public String s2tapi;
	public String s2turl;
	public String t2sapi;
	public String t2surl;
	public String assistantapi;
	public String assistanturl;
	public String assistantworkspaceid;
	public String cloudfuncurl;


    public MfpAccess(String cloudfuncurl, String s2tapi, String s2turl, String t2sapi, String t2surl, String assistantapi, String assistanturl, String assistantworkspaceid){
        this.s2tapi = s2tapi;
        this.s2turl = s2turl;
        this.t2sapi = t2sapi;
        this.t2surl = t2surl;
        this.assistantapi = assistantapi;
        this.assistanturl = assistanturl;
        this.assistantworkspaceid = assistantworkspaceid;
        this.cloudfuncurl = cloudfuncurl;
    }
}
