/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.ibm;

import io.swagger.annotations.Api;
import java.util.logging.Logger;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;
import com.ibm.mfp.adapter.api.AdaptersAPI;

@OAuthSecurity(enabled=false)
@Api(value = "Sample Adapter Resource")
@Path("/resource")
public class AvatarchatbotResource {
	/*
	 * For more info on JAX-RS see
	 * https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
	 */

	// Define logger (Standard java.util.Logger)
	static Logger logger = Logger.getLogger(AvatarchatbotResource.class.getName());

	// Inject the MFP configuration API:
	@Context
	ConfigurationAPI configApi;

	@Context
	AdaptersAPI adaptersAPI;
	
	@GET
	@Path("/mfpapi")
	@Produces("application/json")
	public Response getMfpAccess() throws Exception {
		AvatarchatbotApplication app = adaptersAPI.getJaxRsApplication(AvatarchatbotApplication.class);
		return Response.ok(app.getMfpAccess()).build();
	}



}
