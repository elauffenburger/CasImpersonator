package com.pepperdine.edu.cas;

import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.*;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.pepperdine.edu.cas.*;

public class TestCasImpersonator {

	public static void main(String[] args) {
		try {
			HttpEntity postEntity = MultipartEntityBuilder.create()
										.addBinaryBody("testData", "Hi, I'm test data!".getBytes())
										.addTextBody("moredata", "I'm text data!").build();
			
			HttpResponse response = CasImpersonatorPrincipal.getInstance().makeCall(new CasRequest("username", "password", "https://mysite.myschool.edu/welcome/", "https://casportal.myschool.edu/cas/"), RequestType.POST, ContentType.MultipartFormData, postEntity, false).Response; 
			StringWriter strWriter = new StringWriter();
			IOUtils.copy(response.getEntity().getContent(), strWriter);
			System.out.print(strWriter.toString());
		}catch(Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

}
