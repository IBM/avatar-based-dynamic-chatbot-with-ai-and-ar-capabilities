/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

package com.ibm;

import javax.ws.rs.core.Context;
import java.util.logging.Logger;
import com.ibm.mfp.adapter.api.MFPJAXRSApplication;
import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;

@OAuthSecurity(enabled=false)
public class AvatarchatbotApplication extends MFPJAXRSApplication{

	@Context
	ConfigurationAPI configurationAPI;

	private String s2tapi = "";
	private String s2turl = "";
	private String t2sapi = "";
	private String t2surl = "";
	private String assistantapi = "";
	private String assistanturl= "";
	private String assistantworkspaceid = "";
	private String cloudfuncurl = "";

	static Logger logger = Logger.getLogger(AvatarchatbotApplication.class.getName());

	protected void init() throws Exception {
		logger.info("Adapter initialized!");

		String cloudfuncurl = configurationAPI.getPropertyValue("cloudfuncurl");
		this.cloudfuncurl = cloudfuncurl;

		String s2tapi = configurationAPI.getPropertyValue("s2tapi");
		this.s2tapi = s2tapi;

		String s2turl = configurationAPI.getPropertyValue("s2turl");
		this.s2turl = s2turl;

		String t2sapi = configurationAPI.getPropertyValue("t2sapi");
		this.t2sapi = t2sapi;

		String t2surl = configurationAPI.getPropertyValue("t2surl");
		this.t2surl = t2surl;

		String assistantapi = configurationAPI.getPropertyValue("assistantapi");
		this.assistantapi = assistantapi;

		String assistanturl = configurationAPI.getPropertyValue("assistanturl");
		this.assistanturl = assistanturl;

		String assistantworkspaceid = configurationAPI.getPropertyValue("assistantworkspaceid");
		this.assistantworkspaceid = assistantworkspaceid;
	}
	
	public MfpAccess getMfpAccess(){
		return new MfpAccess(this.cloudfuncurl, this.s2tapi, this.s2turl, this.t2sapi, this.t2surl, this.assistantapi, this.assistanturl, this.assistantworkspaceid);
	}

	protected void destroy() throws Exception {
		logger.info("Adapter destroyed!");
	}
	

	protected String getPackageToScan() {
		//The package of this class will be scanned (recursively) to find JAX-RS resources. 
		//It is also possible to override "getPackagesToScan" method in order to return more than one package for scanning
		return getClass().getPackage().getName();
	}
}
