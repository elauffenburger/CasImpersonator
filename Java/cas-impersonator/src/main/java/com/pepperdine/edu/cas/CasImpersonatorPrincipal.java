package com.pepperdine.edu.cas;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.net.CookieStore;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParamConfig;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.config.*;
import org.apache.commons.lang.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import junit.swingui.StatusLine;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;


public class CasImpersonatorPrincipal {
	private static CasImpersonatorPrincipal casInstance = null;

	protected CasImpersonatorPrincipal() {

	}

	public static CasImpersonatorPrincipal getInstance() {
		if(casInstance == null) {
			casInstance = new CasImpersonatorPrincipal();
		}
		return casInstance;
	}

	/**
	 * Sends a request based on parameters.  Shouldn't be called directly.
	 * @return
	 */
	private HttpResponse sendRequest(HttpClient client, HttpClientContext context, String uri, RequestType rType, ContentType cType, Object content) {
		HttpResponse returnResponse = null;
		try{
			switch (rType) {
			case GET:
				HttpGet getReq = new HttpGet(uri);
				
				returnResponse = client.execute(getReq, context);
				break;
			case POST:
				HttpPost postReq = new HttpPost(uri);
				
				switch (cType) {
				case FormUrlEncoded:
					postReq.setEntity((UrlEncodedFormEntity)content);
					break;
				case MultipartFormData:
					postReq.setEntity((HttpEntity)content);
					break;
				default:
					break;
				}
				
				returnResponse = client.execute(postReq, context);
				break;
			}
		} catch (Exception ex) {

		}
		return returnResponse;
	}

	/**
	 * Primary method to send a request.  Note that cType will be ignored if rType is not applicable (GET).
	 * Currently, supported HTTP methods are GET and POST; these will handle 90% of requests, and so they get priority.
	 * justST is a boolean which will stop execution of the impersonation once an ST is obtained; you can therefore chain
	 * multiple requests if the default makeCall(...) just isn't working for your configuration.
	 * @param request a CasRequest object that contains username, password, target URI, and CAS URI
	 * @param rType a RequestType that determines HTTP method to use
	 * @param cType a ContentType that determines the Content-Type to use (object cast, ultimately)
	 * @param content an object which contains the data to be cast (should be an HttpEntity of some kind, as enumerated by ContentType)
	 * @param justST a boolean which, if set, will stop execution once an ST is obtained
	 * @return a CasImpersonationResult object which contains information about the request
	 */
	public CasImpersonationResult makeCall(CasRequest request, RequestType rType, ContentType cType, Object content, boolean justST) {
		CasImpersonationResult returnResult = null;
		try {
			HttpClient client = null;
			HttpClientContext clientContext = HttpClientContext.create();
			clientContext.setCookieStore(new BasicCookieStore());
			RequestConfig clientConfig = RequestConfig.custom().setCircularRedirectsAllowed(false).setRedirectsEnabled(false).build();
			client = HttpClientBuilder.create().setDefaultRequestConfig(clientConfig).setDefaultCookieStore(clientContext.getCookieStore()).build();
			
			HttpResponse response = sendRequest(client, clientContext, request.uri, rType, cType, content);
			
			while(true) {
				String location = "";
				if(response.getHeaders("Location") != null) {
					location = String.copyValueOf(response.getHeaders("Location")[0].getValue().toCharArray());
				}
				
				if(location.contains(request.casUri)) {
					response = sendRequest(client, clientContext, location, RequestType.GET, ContentType.None, null);
					DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
					builderFactory.setValidating(false);
					builderFactory.setNamespaceAware(false);
					builderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
					builderFactory.setFeature("http://xml.org/sax/features/validation", false);
					builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
					builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
					
					Document CasDocument = builderFactory.newDocumentBuilder().parse(new InputSource(response.getEntity().getContent()));
					
					CasDocument.getDocumentElement().normalize();
					
					XPathFactory xpf = XPathFactory.newInstance();
					XPath xpath = xpf.newXPath();
					
					XPathExpression expression = xpath.compile("//input[@name='lt']");
					Node lt = (Node)expression.evaluate(CasDocument, XPathConstants.NODE);
					lt.getAttributes().getNamedItem("value");
					
					expression = xpath.compile("//input[@name='execution']");
					Node execution = (Node)expression.evaluate(CasDocument, XPathConstants.NODE);
					
					BasicCookieStore tempCookieStore = null;
					try{
						ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
						ObjectOutputStream objStream = new ObjectOutputStream(byteStream);						
						objStream.writeObject(clientContext.getCookieStore());
						objStream.flush();
						objStream.close();
						
						tempCookieStore = (BasicCookieStore)(new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))).readObject();
						
					}catch(Exception ex) {
						ex.printStackTrace(System.out);
					}
					clientConfig = RequestConfig.custom().setRedirectsEnabled(true).setCircularRedirectsAllowed(true).build();
					if(tempCookieStore != null) {
						clientContext.setCookieStore(tempCookieStore);
					}
					client = HttpClientBuilder.create().setDefaultCookieStore(clientContext.getCookieStore()).setDefaultRequestConfig(clientConfig).build();

					Vector<NameValuePair> params = new Vector<NameValuePair>();
					params.add(new BasicNameValuePair("username", request.username));
					params.add(new BasicNameValuePair("password", request.password));
					params.add(new BasicNameValuePair("lt", lt.getAttributes().getNamedItem("value").getNodeValue()));
					params.add(new BasicNameValuePair("execution", execution.getAttributes().getNamedItem("value").getNodeValue()));
					params.add(new BasicNameValuePair("_eventId", "submit"));
					
					UrlEncodedFormEntity formData = new UrlEncodedFormEntity(params, "UTF-8");
					
					response = sendRequest(client, clientContext, location, RequestType.POST, ContentType.FormUrlEncoded, formData);
					
					StringWriter strWriter = new StringWriter();
					IOUtils.copy(response.getEntity().getContent(), strWriter);
					String responseContent = strWriter.toString();
					
					String ST = response.getFirstHeader("Location").getValue().split("\\?ticket=")[1];
					
					if(justST) {
						return new CasImpersonationResult(response, ST);
					}
					
					String targetLocation = response.getFirstHeader("Location").getValue();
					response = sendRequest(client, clientContext, targetLocation, rType, cType, content);
					
					while(response.getStatusLine().getStatusCode() != 200) {
						targetLocation = request.uri;
						response = sendRequest(client, clientContext, targetLocation, rType, cType, content);
					}
					return new CasImpersonationResult(response, ST);
					
				}else{
					response = sendRequest(client, clientContext, location, RequestType.GET, ContentType.None, null);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return returnResult;
	}
}
