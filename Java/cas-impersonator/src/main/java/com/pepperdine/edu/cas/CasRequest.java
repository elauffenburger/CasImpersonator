package com.pepperdine.edu.cas;

import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;

public class CasRequest {
	public String username;
	public String password;
	public String uri;
	public String casUri;
	
	public CasRequest() {
		username = "";
		password = "";
		uri = null;
		casUri = null;
	}
	
	public CasRequest(String u, String p, String URI, String CasURI) {
		username = u;
		password = p;
		uri = URI;
		casUri = CasURI;
	}
}
