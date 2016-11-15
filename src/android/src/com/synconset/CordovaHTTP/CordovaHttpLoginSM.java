/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset;

import android.content.Context;
import android.os.Build;
import android.provider.Telephony;
import android.util.Base64;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieManager;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import static com.github.kevinsawicki.http.HttpRequest.METHOD_GET;
import static com.github.kevinsawicki.http.HttpRequest.METHOD_POST;
import static java.net.HttpURLConnection.HTTP_OK;

public class CordovaHttpLoginSM extends CordovaHttp implements Runnable {
    private String username;
    private String password;
    private String targetUrl;
    private Context context;
    private String SMSESSION;
    private static final boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.PROXY,   //0
            Telephony.Carriers.PORT     //1
    };

    public CordovaHttpLoginSM(String urlString, Map<?, ?> params, Map<String, String> headers,
                              CallbackContext callbackContext, Context context) {
        super(urlString, params, headers, callbackContext);
        this.username = (String) params.get("username");
        this.password = (String) params.get("password");
        this.targetUrl = (String) params.get("targetUrl");
        this.context = context;
    }

    @Override
    public void run() {
        try {
            // Calculate the authorization header
            String loginInfo = username + ":" + password;
            loginInfo = "Basic " + Base64.encodeToString(loginInfo.getBytes(), Base64.NO_WRAP);

            HttpRequest.proxyHost("16.22.89.78");
            HttpRequest.proxyPort(8888);
            HttpRequest request = new HttpRequest(this.getUrlString(), METHOD_POST);
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

                this.getCallbackContext().success(response);
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
            body = reqFollow.body(CHARSET);
            response.put("status", code);
            response.put("data", body);
        }
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
}