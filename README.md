# Impersonation for Central Authentication Service (CAS)
###Author/Person to blame: 
Eric Lauffenburger

## Introduction

This is the repository for a simple implementation of a CAS impersonation client -- it is not intended to bypass or subvert the purpose of CAS, merely to make simple calls that will pass-through the initial security layer.

## Use Cases

Let's take the following example:

- Webapp **Alice** wants to POST data with _Content-Type: application/x-www-form-urlencoded_ to Service **Sam**
- **Sam** is secured with *__CAS__*
- We don't want **Bob**, a user, to have access to **Sam's** API (meaning **Sam** cannot use a _Proxy Ticket_ generated from **Bob** logging into **Alice**)
- We have a service account, **Charlie**, whose sole purpose is to have access to **Sam's** API

Using _**CasImpersonation**_, we can send that data to **Sam** from **Alice** using **Charlie's** credentials; **Bob** will never see a redirect, will not have access to **Sam's** API, but can still take advantage of specific features of the API without ever getting direct control of them.

## Download

Because this project provides both a C# and Java implementation, I'm centralizing its existence here rather than on NuGet and Maven (or equivalent SCM). Just download the package that fits your language flavor!

## Build

I'm including the Maven package for Java and .sln files for C#, so it should be pretty straightforward to compile and run.

## License

MIT License, just credit me as the author, etc.

## Considerations:

**There's not what I would call...robust exception handling -- _if your credentials are bad you will see some weird stuff happen_**; I'll fix this in a future update.

## Usage Guide

### C Sharp

```csharp
//includes...
using CasImpersonator;
//additional stuff...

static void Main(string[] args)
        {
            CasRequest request = new CasRequest()
            {
                Username = "username",
                Password = "password",
                URI = new Uri(@"https://mysite.myschool.edu/welcome/"),
                CasURI = new Uri(@"https://casportal.myschool.edu/cas/")
            };
            CasImpersonationResult result = CasImpersonator.CasImpersonatorPrincipal.MakeCall(request, CasImpersonator.CasTypes.RequestType.GET, CasImpersonator.CasTypes.ContentType.None, null, false);
            System.Console.Write((new System.IO.StreamReader(result.Response.Content.ReadAsStreamAsync().Result)).ReadToEnd());
            while (Console.ReadKey().Key != System.ConsoleKey.Enter)
            {

            }
        }
```

Make sure that the content you pass is either:
- **FormUrlEncodedContent**
- **MultipartFormDataContent**
- **_null_**

### Java

```java
//includes...
import com.pepperdine.edu.cas.*;
//additional stuff...

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
```

Make sure that the content you pass is an **HttpEntity** of type either:
- **UrlEncodedFormEntity**
- **MultipartEntity**
- **_null_**