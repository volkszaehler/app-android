package org.volkszaehler.volkszaehlerapp;

import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.List;

class ServiceHandler {

    public final static int GET = 1;
    private final static int POST = 2;

    public ServiceHandler() {

    }

    public String makeServiceCall(String url, int method) {
        //fix for issue 8 https://github.com/volkszaehler/app-android/issues/8
        URL newUrl = null;
        try {
            newUrl = new URL(url);
            URI uri = new URI(newUrl.getProtocol(), newUrl.getUserInfo(), IDN.toASCII(newUrl.getHost()), newUrl.getPort(), newUrl.getPath(), newUrl.getQuery(), newUrl.getRef());
            newUrl = uri.toURL();
        } catch (MalformedURLException e) {
            Log.e("ServiceHandler", "malformed URL: " + url);
            e.printStackTrace();
        } catch (URISyntaxException e) {
            Log.e("ServiceHandler", "wrong URL Syntax : " + newUrl);
            e.printStackTrace();
        }

        return this.makeServiceCall(newUrl.toString(), method, null);
    }

    private HttpClient getNewHttpClient(String url) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            URL realURL = new URL(url);

            SchemeRegistry registry = new SchemeRegistry();
            if (url.startsWith("http:")) {
                int port = realURL.getPort() != -1 ? realURL.getPort() : 80;
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port));
            } else if (url.startsWith("https:")) {
                int port = realURL.getPort() != -1 ? realURL.getPort() : 443;
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                registry.register(new Scheme("https", sf, port));
            }

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    /**
     * Making service call
     *
     * @url - url to make request
     * @method - http request method
     * @params - http request params
     */
    private String makeServiceCall(String url, int method, List<NameValuePair> params) {
        return this.makeServiceCall(url, method, null, null, null);
    }

    public String makeServiceCall(String url, int method, List<NameValuePair> params, String uname, String pwd) {
        String response = "";

        try {
            // http client
            HttpClient httpClient = getNewHttpClient(url);
            HttpEntity httpEntity;
            HttpResponse httpResponse = null;

            // Checking http request method type
            if (method == POST) {
                HttpPost httpPost = new HttpPost(url);
                if (uname != null) {
                    final String basicAuth = "Basic " + Base64.encodeToString((uname + ":" + pwd).getBytes(), Base64.NO_WRAP);
                    httpPost.setHeader("Authorization", basicAuth);
                }
                // adding post params
                if (params != null) {
                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                }

                httpResponse = httpClient.execute(httpPost);

            } else if (method == GET) {
                // appending params to url
                if (params != null) {
                    String paramString = URLEncodedUtils.format(params, "utf-8");
                    url += "?" + paramString;
                }
                HttpGet httpGet = new HttpGet(url);
                if (uname != null) {
                    final String basicAuth = "Basic " + Base64.encodeToString((uname + ":" + pwd).getBytes(), Base64.NO_WRAP);
                    httpGet.setHeader("Authorization", basicAuth);
                }
                httpResponse = httpClient.execute(httpGet);
            }

            httpEntity = httpResponse.getEntity();
            response = EntityUtils.toString(httpEntity);

            if (200 == (httpResponse.getStatusLine().getStatusCode())) {
                try {
                    JSONObject jsonObj = new JSONObject(response);
                } catch (JSONException e) {
                    //no JSON response
                    response = "Error: " + response;
                    e.printStackTrace();
                }
            } else {
                //error occurred
                response = "Error: " + httpResponse.getStatusLine() + " " + response;
            }


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return response;
    }
}