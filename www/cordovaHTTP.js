/*global angular*/

/*
 * An HTTP Plugin for PhoneGap.
 */

var exec = require('cordova/exec');

var http = {
    useBasicAuth: function(username, password, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "useBasicAuth", [username, password]);
    },
	loginSiteMinder: function(username, password, targetUrl, url, success, failure){
			var params = {username: username, password: password, targetUrl: targetUrl};	
		return exec(success, failure, "CordovaHttpPlugin", "loginSiteMinder", [url, params]);
	},
	clearAllCookies: function(success,failure){
			return exec(success,failure,"CordovaHttpPlugin", "clearAllCookies",[]);
	},
    setHeader: function(header, value, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "setHeader", [header, value]);
    },
    enableSSLPinning: function(enable, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "enableSSLPinning", [enable]);
    },
    acceptAllCerts: function(allow, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "acceptAllCerts", [allow]);
    },
    post: function(url, params, headers, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "post", [url, params, headers]);
    },
    get: function(url, params, headers, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "get", [url, params, headers]);
    },
    put: function (url, params, headers, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "put", [url, params, headers]);
    },
    delete: function (url, params, headers, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "delete", [url, params, headers]);
    },
    uploadFile: function(url, params, headers, filePath, name, success, failure) {
        return exec(success, failure, "CordovaHttpPlugin", "uploadFile", [url, params, headers, filePath, name]);
    },
    downloadFile: function(url, params, headers, filePath, success, failure) {
        /*
         *
         * Licensed to the Apache Software Foundation (ASF) under one
         * or more contributor license agreements.  See the NOTICE file
         * distributed with this work for additional information
         * regarding copyright ownership.  The ASF licenses this file
         * to you under the Apache License, Version 2.0 (the
         * "License"); you may not use this file except in compliance
         * with the License.  You may obtain a copy of the License at
         *
         *   http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing,
         * software distributed under the License is distributed on an
         * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
         * KIND, either express or implied.  See the License for the
         * specific language governing permissions and limitations
         * under the License.
         *
         * Modified by Andrew Stephan for Sync OnSet
         *
        */
        var win = function(result) {
            var entry = new (require('org.apache.cordova.file.FileEntry'))();
            entry.isDirectory = false;
            entry.isFile = true;
            entry.name = result.file.name;
            entry.fullPath = result.file.fullPath;
            success(entry);
        };
        return exec(win, failure, "CordovaHttpPlugin", "downloadFile", [url, params, headers, filePath]);
    }
};

module.exports = http;
	var CordovaHTTPAngular = function ($timeout, $q){
		this.$timeout = $timeout;
		this.$q = $q;
		
		this.makePromise = function(fn, args, async) {
				var deferred = $q.defer();
				
				var success = function(response) {
					if (async) {
						$timeout(function() {
							deferred.resolve(response);
						});
					} else {
						deferred.resolve(response);
					}
				};
				
				var fail = function(response) {
					if (async) {
						$timeout(function() {
							deferred.reject(response);
						});
					} else {
						deferred.reject(response);
					}
				};
				
				args.push(success);
				args.push(fail);
				
				fn.apply(http, args);
				
				return deferred.promise;
			}
	};
	
	CordovaHTTPAngular.prototype.useBasicAuth= function(username, password) {
					return this.makePromise(http.useBasicAuth, [username, password]);
				};
	CordovaHTTPAngular.prototype.loginSiteMinder= function(username, password, url, targetUrl){
					return this.makePromise(http.loginSiteMinder, [username,password,targetUrl,url],true);
				};
	CordovaHTTPAngular.prototype.clearAllCookies= function(){
					return this.makePromise(http.clearAllCookies, []);
				};
	CordovaHTTPAngular.prototype.setHeader= function(header, value) {
					return this.makePromise(http.setHeader, [header, value]);
				};
	CordovaHTTPAngular.prototype.enableSSLPinning= function(enable) {
					return this.makePromise(http.enableSSLPinning, [enable]);
				};
	CordovaHTTPAngular.prototype.acceptAllCerts= function(allow) {
					return this.makePromise(http.acceptAllCerts, [allow]);
				};
	CordovaHTTPAngular.prototype.post= function(url, params, headers) {
					return this.makePromise(http.post, [url, params, headers], true);
				};
	CordovaHTTPAngular.prototype.get= function(url, params, headers) {
					return this.makePromise(http.get, [url, params, headers], true);
				};
	CordovaHTTPAngular.prototype.uploadFile= function(url, params, headers, filePath, name) {
					return this.makePromise(http.uploadFile, [url, params, headers, filePath, name], true);
				};
	CordovaHTTPAngular.prototype.downloadFile= function(url, params, headers, filePath) {
					return makePromise(http.downloadFile, [url, params, headers, filePath], true);
				};

	/**
	 * Angular service provider
	 */
	 if (typeof angular !== "undefined") {
		 angular.module('cordovaHTTP', []).provider('$cordovaHTTP', function() {
			/// <summary>
			/// Provider used by angular to get a WebServices object
			/// </summary>

			this.$get = function ($timeout,$q) {
				/// <summary>
				/// Get a WebService object
				/// </summary>
				return new CordovaHTTPAngular($timeout,$q);
			};
			}
		);
	 }
	else {
		window.cordovaHTTP = http;
	}