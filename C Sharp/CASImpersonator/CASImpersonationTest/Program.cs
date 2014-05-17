using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using CasImpersonator;
using System.Net;
using System.Net.Http;

namespace CasImpersonationTest
{
    class Program
    {
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
    }
}
