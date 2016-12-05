/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import static com.github.kevinsawicki.http.HttpRequest.METHOD_GET;
import static com.github.kevinsawicki.http.HttpRequest.METHOD_POST;
import static java.net.HttpURLConnection.HTTP_OK;

public class CordovaHttpLoginSM extends CordovaHttp implements Runnable {
    private Map<String, String> headers;
    private String username;
    private String password;
    private String targetUrl;
    private Context context;
    private ConnectivityManager cmngr;
    private WifiManager wifimngr;
    private TelephonyManager telmngr;
    private String SMSESSION;

    class Proxy {
        String host;
        int port;
    }
    /*
     * Information of all APNs
     * Details can be found in com.android.providers.telephony.TelephonyProvider
     */
    public static final Uri PREFERED_APN_URI =
            Uri.parse("content://telephony/carriers/preferapn");

    public CordovaHttpLoginSM(String urlString, Map<?, ?> params, Map<String, String> headers,
                              CallbackContext callbackContext, Context context) {
        super(urlString, params, headers, callbackContext);
        this.username = (String) params.get("username");
        this.password = (String) params.get("password");
        this.targetUrl = (String) params.get("targetUrl");
        this.context = context;
        this.cmngr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifimngr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.headers = headers;
    }

    @Override
    public void run() {
        try {
            // Calculate the authorization header
            String loginInfo = username + ":" + password;
            loginInfo = "Basic " + Base64.encodeToString(loginInfo.getBytes(), Base64.NO_WRAP);

            // Globally set the proxy, all the future POST and GET will use it
            if(wifimngr.isWifiEnabled()) {
                Proxy proxy = getProxyHost();
                if (proxy != null) {
                    HttpRequest.proxyHost(proxy.host);
                    HttpRequest.proxyPort(proxy.port);
                }
            }

            HttpRequest request = new HttpRequest(this.getUrlString(), METHOD_POST);
            request.header(HttpRequest.HEADER_CONTENT_TYPE, HttpRequest.CONTENT_TYPE_JSON);
            request.getConnection().setInstanceFollowRedirects(false);
            request.acceptCharset(CHARSET);
            request.authorization(loginInfo);
            request.form("USER", username);
            request.form("PASSWORD", password);
            request.form("SMENC", "ISO-8859-1");
            request.form("TARGET", targetUrl);
            request.form("smauthreason", "0");
            request.form("smretries", "1");
            setupSecurity(request);

            int code = request.code();
            String body = request.body(CHARSET);
            JSONObject response = new JSONObject();

            if (code == 302) {
                String formCred = getCookie(request, "FORMCRED");
                response = followTargetUrl(formCred);
                if(!SMSESSION.isEmpty()) {
                    this.getCallbackContext().success(response);
                }
                else {
                    response.put("error - during SM redirect to the target URL",response);
                    this.getCallbackContext().error(response);
                }
            } else {
                response.put("error - not authenticated", body);
                this.getCallbackContext().error(response);
            }
        } catch (JSONException e) {
            this.respondWithError("There was an error generating the response");
        } catch (HttpRequestException e) {
            if (e.getCause() instanceof UnknownHostException) {
                this.respondWithError(0, "The host could not be resolved");
            } else if (e.getCause() instanceof SSLHandshakeException) {
                this.respondWithError("SSL handshake failed");
            } else {
                this.respondWithError("There was an error with the request");
            }
        }
    }

    /**
     * A site minder authentication require a target URL, this method
     * is made to follow that target URL and set the SMSession cookie
     *
     * @param formCred
     * @return
     * @throws JSONException
     */
    private JSONObject followTargetUrl(String formCred) throws JSONException {
        String body;
        JSONObject response = new JSONObject();
        HttpRequest reqFollow = new HttpRequest(this.targetUrl, METHOD_GET);
        setupSecurity(reqFollow);

        if (formCred != null) {
            reqFollow.header("Cookie", formCred);
        }

        int code = reqFollow.code();

        if (code == HTTP_OK) {
            SMSESSION = getCookie(reqFollow, "SMSESSION");
            this.headers.put("Cookie",SMSESSION);
        }

        body = reqFollow.body(CHARSET);
        response.put("status", code);
        response.put("data", body);
        return response;
    }

    /**
     * Get a cookie from a request
     *
     * @param request    : A HttpRequest object
     * @param cookieName : Name of the cookie to find
     * @return The cookie or null
     */
    private static String getCookie(HttpRequest request, String cookieName) {
        String cookie = null;

        Map<String, List<String>> headerFields = request.getConnection().getHeaderFields();
        List<String> cookies = headerFields.get("set-cookie");
        ListIterator<String> iterator = cookies.listIterator();

        while (iterator.hasNext()) {
            String current = iterator.next();
            if (current.contains(cookieName)) {
                cookie = current;
                break;
            }
        }

        return cookie;
    }

    /**
     * Retreve ProxyInfo object form the current system connection
     * does not work for APN connections
     *
     * @return
     */
    private Proxy getProxyHost() {
        Proxy proxy = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network net = cmngr.getActiveNetwork();
            LinkProperties props = cmngr.getLinkProperties(net);
            ProxyInfo proxyInfo = props.getHttpProxy();
            if(proxyInfo != null){
                proxy = new Proxy();
                proxy.host = proxyInfo.getHost();
                proxy.port = proxyInfo.getPort();
            }
        } else {
            String host;
            int port;
            host = System.getProperty("http.proxyHost");
            port = Integer.parseInt(System.getProperty("http.proxyPort"));

            if(host !=null && port != 0){
                proxy = new Proxy();
                proxy.host = host;
                proxy.port = port;
            }
        }

        return proxy;
    }
}