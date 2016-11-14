/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset;
import java.net.UnknownHostException;
import java.util.Map;
import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;
import javax.net.ssl.SSLHandshakeException;
import android.util.Base64;
import android.util.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
 

public class CordovaHttpLoginSM extends CordovaHttp implements Runnable {
	private String username;
	private String password;
	private String targetUrl;
	
	public CordovaHttpLoginSM(String urlString, Map<?, ?> params,Map<String, String> headers, CallbackContext callbackContext) {
		super(urlString, params, headers, callbackContext);
		this.username = (String) params.get("username");
		this.password = (String) params.get("password");
		this.targetUrl = (String) params.get("targetUrl");
	}

	@Override
    public void run() {
        try {
			// Calculate the authorization header
            String loginInfo = username + ":" + password;
            loginInfo = "Basic " + Base64.encodeToString(loginInfo.getBytes(), Base64.NO_WRAP);
			
             HttpRequest request = HttpRequest.post(this.getUrlString());
             this.setupSecurity(request);
             request.acceptCharset(CHARSET);
             request.authorization(loginInfo);
             //request.form(this.getParams());
             request.form("USER",username);
             request.form("PASSWORD",password);
             request.form("SMENC","ISO-8859-1");
             request.form("TARGET",targetUrl);
             request.form("smauthreason","0");
             request.form("smretries","1");      
             int code = request.code();
             String body = request.body(CHARSET);
             JSONObject response = new JSONObject();
             response.put("status", code);
             
             if (code >= 200 && code < 300) {
                 response.put("data", body);
           
             this.getCallbackContext().success(response);
             } else {
                 response.put("error", body);
                 this.getCallbackContext().error(response);
             }
	    } catch (JSONException e) {
	        this.respondWithError("There was an error generating the response");
	    }  catch (HttpRequestException e) {
	        if (e.getCause() instanceof UnknownHostException) {
	            this.respondWithError(0, "The host could not be resolved");
	        } else if (e.getCause() instanceof SSLHandshakeException) {
	            this.respondWithError("SSL handshake failed");
	        } else {
	            this.respondWithError("There was an error with the request");
	        }
	    }
	}
  }