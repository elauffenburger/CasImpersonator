package com.pepperdine.edu.cas;

import org.apache.http.HttpResponse;

public class CasImpersonationResult {
	public HttpResponse Response;
	public String ST;
	
	public CasImpersonationResult() {
		
	}
	
	public CasImpersonationResult(HttpResponse response, String st) {
		Response = response;
		ST = st;
	}
}
