using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace CasImpersonator
{
    public class CasImpersonationResult
    {
        public HttpResponseMessage Response;
        public String ST;

        public CasImpersonationResult()
        {

        }

        public CasImpersonationResult(HttpResponseMessage response, String st)
        {
            Response = response;
            ST = st;
        }
    }
}
