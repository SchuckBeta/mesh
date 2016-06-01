package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

public class JWTAuthentication extends AbstractAuthenticationProvider{

	private String token;
	private String authHeader;
	private Observable<GenericMessageResponse> loginRequest;
	
	public JWTAuthentication() {
	}

	public JWTAuthentication(RoutingContext context) {
		super();
		this.authHeader = context.request().getHeader("Authorization");
	}
	
	@Override
	public Observable<Void> addAuthenticationInformation(HttpClientRequest request) {
		//TODO: request new Token when old one expires
		
		if (authHeader != null) {
			request.headers().add("Authorization", "Bearer " + token);
			return Observable.just(null);
		} else if (loginRequest == null) {
			return Observable.just(null);
		} else {
			return loginRequest.map(x -> {
				request.headers().add("Authorization", "Bearer " + token);
				return null;
			});
		}
	}

	@Override
	public Observable<GenericMessageResponse> login(HttpClient client) {
		this.loginRequest = Observable.create(sub -> {
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setUsername(getUsername());
			loginRequest.setPassword(getPassword());
			
			MeshRestRequestUtil.handleRequest(HttpMethod.POST, "/auth/login", TokenResponse.class, loginRequest, client, null).setHandler(rh -> {
				if (rh.failed()) {
					sub.onError(rh.cause());
				} else {
					token = rh.result().getToken();
					sub.onNext(new GenericMessageResponse("OK"));
					sub.onCompleted();
				}
			});
		});
		this.loginRequest = this.loginRequest.cache(1);
		return this.loginRequest;
	}

	@Override
	public Observable<GenericMessageResponse> logout(HttpClient client) {
		token = null;
		loginRequest = null;
		//No need call any endpoint in JWT
		return Observable.just(new GenericMessageResponse("OK"));
	}

}