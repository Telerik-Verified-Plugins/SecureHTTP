/**
 * A HTTP plugin for Cordova / Phonegap
 */
package mobi.conta;

import java.net.UnknownHostException;
import java.util.Map;

import java.lang.Object;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;


import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLHandshakeException;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
 
public class CordovaHttpDeleteJson extends CordovaHttp implements Runnable {
    public CordovaHttpDeleteJson(String urlString, JSONObject jsonObj, Map<String, String> headers, CallbackContext callbackContext) {
        super(urlString, jsonObj, headers, callbackContext);
    }
    
    @Override
    public void run() {
        try {
            HttpRequest request = HttpRequest.delete(this.getUrlString());
            this.setupSecurity(request);
            request.headers(this.getHeaders());
            request.accept("application/json");
            request.contentType(HttpRequest.CONTENT_TYPE_JSON);
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
                this.respondWithError("There was an error with the request" + e.getMessage());
            }
        }
    }
}
