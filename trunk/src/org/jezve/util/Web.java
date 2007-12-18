/**
* Copyright (c) 2007-2008, jezve.org and its Contributors
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the jezve.org nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY jezve.org AND SOFTWARE CONTRIBUTORS ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL jezve.org or CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.jezve.util;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.List;

public final class Web {

    public static void openUrl(String url) throws IOException {
        if (Platform.isMac()) {
            Call.callStatic("com.apple.eio.FileManager.openURL", new Object[]{url});
        } else {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
        }
    }

    public static void sendMail(String email, String title, String body) throws IOException {
        // experimantal numbers. 600 for google gmail notifier... :-(
        final int MAX_LENGTH = Platform.isWindows() ? 1200 : 600;
        if (body.length() > MAX_LENGTH) {
            body = body.substring(0, MAX_LENGTH);
        }
        body = escapeRFC2368(body);
        title = escapeRFC2368(title);
        openUrl("mailto:" + email + "?subject=" + title + "&body=" + body);
    }

    /** HTTP Headers are case-insensitive */

    public static class Headers extends HashMap {

        public Headers() {
        }

        public Headers(Map map) {
            putAll(map);
        }

        public Object get(Object key) {
            return super.get(key != null ? ((String)key).toLowerCase() : null);
        }

        public Object put(Object key, Object value) {
            return super.put(key != null ? ((String)key).toLowerCase() : null, value);
        }

    }

    /**
     * HTTP GET
     * @param url to get from
     * @param headers Map of none-null String -> String value pairs (params itself can be null)
     * @param nvp name value pairs (can be null)
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    public static boolean getFromUrl(String url, Headers headers, Map nvp,
            Headers reply, ByteArrayOutputStream body) throws IOException {
        return httpGetPost(false, url, headers, nvp, "", reply, body);
    }

    /**
     * HTTP POST
     * @param url to post to
     * @param headers Map of none-null String -> String value pairs
     * @param nvp name value pairs (can be null) values also can be null
     * @param content to send to server (can be null)
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    public static boolean postToUrl(String url, Headers headers, Map nvp, String content,
            Headers reply, ByteArrayOutputStream body) throws IOException {
        return httpGetPost(true, url, headers, nvp, content == null ? "" : content, reply, body);
    }

    /**
     * HTTP GET/POST
     * @param post true if POST is requested
     * @param url to get from
     * @param headers Map of none-null String -> String value pairs
     *        header names are case sensitive keep them lowercase
     * @param nvp name value pairs (can be null), values can be null too.
     *        values will be URLEncoded upon get/post.
     * @param content to send to server
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    private static boolean httpGetPost(boolean post, String url, Headers headers, Map nvp,
            String content, Headers reply, ByteArrayOutputStream body) throws IOException {
        assert content != null;
        URLConnection conn = new URL(url).openConnection();
        conn.setUseCaches(false);
        conn.setDoInput(true);
        if (post) {
            conn.setDoOutput(true);
        }
        conn.setDefaultUseCaches(false);
        conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("user-agent", "Mozilla/4.0");
        conn.setRequestProperty("cache-control", "max-age=0");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("cache-control", "no-store");
        conn.setRequestProperty("pragma", "no-cache");
        conn.setRequestProperty("accept", "text/plain, */*");
        conn.setRequestProperty("accept-language", "en-us,en;q=0.5");
        conn.setRequestProperty("accept-charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        // "chunked", "identity", "gzip", "compress", "deflate"
        conn.setRequestProperty("transfer-coding", "identity");
        if (headers != null) {
            Headers hdr = new Headers(headers);
            for (Iterator i = hdr.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry)i.next();
                assert e.getKey() instanceof String;
                assert e.getValue() instanceof String;
                conn.setRequestProperty((String)e.getKey(), (String)e.getValue());
            }
        }
        if (nvp != null) {
            for (Iterator i = nvp.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry)i.next();
                assert e.getKey() instanceof String;
                assert e.getValue() instanceof String;
                String v = (String)e.getValue();
                content += e.getKey() + "=" + (v == null ? "" : URLEncoder.encode(v, "UTF8"));
                if (i.hasNext()) {
                    content += "&";
                }
            }
        }
        if (post) {
            conn.setRequestProperty("Content-Length", "" + content.length());
            DataOutputStream output = new DataOutputStream(conn.getOutputStream());
            if (content.length() > 0) {
                output.writeBytes(content);
            }
            output.flush();
            IO.close(output);
        }
        Map fields = new Headers(conn.getHeaderFields());
        if (reply != null) {
            reply.putAll(fields);
        }
        String response = getHttpReplyHeaderString(fields, null);
        DataInputStream input = new DataInputStream(conn.getInputStream());
        int len = (int)getHttpReplyHeaderLong(fields, "Content-Length");
        if (len > 0) {
            byte[] buf = new byte[len];
            input.readFully(buf);
            if (body != null) body.write(buf);
        } else if (input.available() >= 0) {
            byte[] buf = new byte[1500];
            while (input.available() >= 0) {
                int k = input.read(buf);
                if (k < 0) {
                    break;
                } else if (k > 0 && body != null) {
                    body.write(buf, 0, k);
                }
            }
        }
        IO.close(input);
        return responseCode(response) == 200; // "HTTP/1.1 200 OK"
    }

    private static String getHttpReplyHeaderString(Map fields, Object key) {
        if (fields == null) {
            return "";
        }
        Object value = fields.get(key);
        if (!(value instanceof List)) {
            return "";
        }
        List list = (List)value;
        if (list.size() <= 0) {
            return "";
        }
        return (String)list.get(0);
    }

    private static long getHttpReplyHeaderLong(Map fields, Object key) {
        String s = getHttpReplyHeaderString(fields, key);
        try {
            return s.length() > 0 ? Long.decode(s).longValue() : -1;
        } catch (NumberFormatException x) {
            return -1;
        }
    }

    private static int responseCode(String s) {
        // simple parser for : "HTTP/1.1 200 OK"
        if (s == null) return -1;
        StringTokenizer st = new StringTokenizer(s);
        if (!st.hasMoreTokens()) return -1;
        st.nextToken(); // HTTP/1.1
        if (!st.hasMoreTokens()) return -1;
        try{
            return Integer.decode(st.nextToken()).intValue();
        } catch (NumberFormatException x) {
            return -1;
        }
    }

    private static String escapeRFC2368(String s) {
        try {
            StringBuffer r = new StringBuffer(s.length());
            byte[] bytes = s.getBytes("UTF8");
            for (int i = 0; i < bytes.length; i++) {
                char ch = (char)(((int)bytes[i]) & 0xFF);
                if (0 <= ch && ch < 0x7F && Character.isLetterOrDigit(ch)) {
                    r.append(ch);
                } else {
                    String x = Integer.toHexString(ch);
                    r.append(x.length() == 1 ? "%0" + x : "%" + x);
                }
            }
            return r.toString();
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

}
