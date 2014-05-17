using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using System.Web;
using System.Web.Http;
using System.Net;
using System.Net.Http;
using HtmlAgilityPack;
using System.Runtime.Serialization.Formatters.Binary;

namespace CasImpersonator
{
    public static class CasImpersonatorPrincipal
    {

        private static HttpResponseMessage SendRequest(HttpClient client, string uri, CasTypes.RequestType rType, CasTypes.ContentType cType, object content) {
            HttpResponseMessage responseMessage = new HttpResponseMessage();
            switch (rType)
            {
                case CasTypes.RequestType.GET:
                    responseMessage = client.GetAsync(uri).Result;
                    break;
                case CasTypes.RequestType.POST:
                    switch (cType)
                    {
                        case CasTypes.ContentType.FormUrlEncoded:
                            responseMessage = client.PostAsync(uri, (FormUrlEncodedContent)content).Result;
                            break;
                        case CasTypes.ContentType.MultipartFormData:
                            responseMessage = client.PostAsync(uri, (MultipartFormDataContent)content).Result;
                            break;
                        default:

                            break;
                    }
                    break;
            }
            return responseMessage;
        }

        /// <summary>
        /// Makes a stateless call to the specified address and handles stateful CAS auth
        /// </summary>
        /// <param name="request"></param>
        /// <param name="rType"></param>
        /// <param name="cType"></param>
        /// <param name="content"></param>
        /// <param name="justST"></param>
        public static CasImpersonationResult MakeCall(CasRequest request, CasTypes.RequestType rType, CasTypes.ContentType cType, object content, bool justST)
        {
            HttpClientHandler handler = new HttpClientHandler()
            {
                AllowAutoRedirect = false
            };
            handler.CookieContainer = new CookieContainer();

            HttpClient client = new HttpClient(handler);
            HttpResponseMessage responseMessage = SendRequest(client, request.URI.AbsoluteUri, rType, cType, content);

            while (true)
            {
                string Location = "";
                if (responseMessage.Headers.Contains("Location"))
                {
                    Location = responseMessage.Headers.Location.AbsoluteUri;
                }
                string ResponseContent = (new System.IO.StreamReader(responseMessage.Content.ReadAsStreamAsync().Result)).ReadToEnd();
                if (responseMessage.StatusCode == HttpStatusCode.Found || responseMessage.StatusCode == HttpStatusCode.Redirect)
                {
                    if (Location.Contains(request.CasURI.AbsoluteUri))
                    {
                        //We need to authenticate through CAS -- get lt & execution from page
                        //Send another request to this new url
                        responseMessage = SendRequest(client, Location, CasTypes.RequestType.GET, CasTypes.ContentType.None, null);
                        string CasContent = (new System.IO.StreamReader(responseMessage.Content.ReadAsStreamAsync().Result)).ReadToEnd();
                        HtmlDocument CasDocument = new HtmlDocument();
                        CasDocument.LoadHtml(CasContent);

                        string lt = CasDocument.DocumentNode.SelectSingleNode(@"//input[@name='lt']").Attributes["value"].Value;
                        string execution = CasDocument.DocumentNode.SelectSingleNode(@"//input[@name='execution']").Attributes["value"].Value;

                        List<KeyValuePair<string, string>> paramsList = new List<KeyValuePair<string, string>>();
                        paramsList.Add(new KeyValuePair<string, string>("username", request.Username));
                        paramsList.Add(new KeyValuePair<string, string>("password", request.Password));
                        paramsList.Add(new KeyValuePair<string, string>("lt", lt));
                        paramsList.Add(new KeyValuePair<string, string>("execution", execution));
                        paramsList.Add(new KeyValuePair<string, string>("_eventId", "submit"));

                        FormUrlEncodedContent CasData = new FormUrlEncodedContent(paramsList);

                        responseMessage = SendRequest(client, Location, CasTypes.RequestType.POST, CasTypes.ContentType.FormUrlEncoded, CasData);

                        string tempLocation = responseMessage.Headers.Location.AbsoluteUri.ToString();
                        string splitPattern = "?ticket=";

                        string ST = tempLocation.Substring(tempLocation.IndexOf(splitPattern)+splitPattern.Length);

                        if (justST)
                        {
                            return new CasImpersonationResult()
                            {
                                Response = responseMessage,
                                ST = ST
                            };
                        }

                        //We're now ready to send out our request; the next 200 we get should mean that we got through auth

                        HttpClientHandler newHandler = new HttpClientHandler();
                        using (System.IO.MemoryStream tempStream = new System.IO.MemoryStream())
                        {
                            BinaryFormatter tempSerializer = new BinaryFormatter();
                            tempSerializer.Serialize(tempStream, handler.CookieContainer);
                            tempStream.Position = 0;
                            newHandler.CookieContainer = (CookieContainer)tempSerializer.Deserialize(tempStream);
                        }

                        newHandler.AllowAutoRedirect = true;
                        client = new HttpClient(newHandler);

                        return new CasImpersonationResult()
                        {
                            Response = SendRequest(client, request.URI.AbsoluteUri, rType, cType, content),
                            ST = ST
                        };
                    }
                    else
                    {
                        responseMessage = SendRequest(client, Location, CasTypes.RequestType.GET, CasTypes.ContentType.None, null);
                    }
                }
            }

        }
    }
}
