using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CasImpersonator
{
    public static class CasTypes
    {
        public enum RequestType
        {
            GET,
            POST
        };

        public enum ContentType
        {
            FormUrlEncoded,
            MultipartFormData,
            None
        };
    }
}
