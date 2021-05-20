/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.web.http;

import java.util.Base64;
import java.util.Collections;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CookieHttpSessionIdResolver}.
 */
class CookieHttpSessionIdResolverTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private CookieHttpSessionIdResolver strategy;

	private String cookieName;

	private Session session;

	@BeforeEach
	void setup() {
		this.cookieName = "SESSION";
		this.session = new MapSession();
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.strategy = new CookieHttpSessionIdResolver();
	}

	@Test
	void getRequestedSessionIdNull() {
		assertThat(this.strategy.resolveSessionIds(this.request)).isEmpty();
	}

	@Test
	void getRequestedSessionIdNotNull() {
		setSessionCookie(this.session.getId());
		assertThat(this.strategy.resolveSessionIds(this.request))
				.isEqualTo(Collections.singletonList(this.session.getId()));
	}

	@Test
	void getRequestedSessionIdNotNullCustomCookieName() {
		setCookieName("CUSTOM");
		setSessionCookie(this.session.getId());
		assertThat(this.strategy.resolveSessionIds(this.request))
				.isEqualTo(Collections.singletonList(this.session.getId()));
	}

	@Test
	void onNewSession() {
		this.strategy.setSessionId(this.request, this.response, this.session.getId());
		assertThat(getSessionId()).isEqualTo(this.session.getId());
	}

	@Test
	void onNewSessionTwiceSameId() {
		this.strategy.setSessionId(this.request, this.response, this.session.getId());
		this.strategy.setSessionId(this.request, this.response, this.session.getId());

		assertThat(this.response.getCookies()).hasSize(1);
	}

	@Test
	void onNewSessionTwiceNewId() {
		Session newSession = new MapSession();

		this.strategy.setSessionId(this.request, this.response, this.session.getId());
		this.strategy.setSessionId(this.request, this.response, newSession.getId());

		Cookie[] cookies = this.response.getCookies();
		assertThat(cookies).hasSize(2);

		assertThat(base64Decode(cookies[0].getValue())).isEqualTo(this.session.getId());
		assertThat(base64Decode(cookies[1].getValue())).isEqualTo(newSession.getId());
	}

	@Test
	void onNewSessionCookiePath() {
		this.request.setContextPath("/somethingunique");
		this.strategy.setSessionId(this.request, this.response, this.session.getId());

		Cookie sessionCookie = this.response.getCookie(this.cookieName);
		assertThat(sessionCookie.getPath()).isEqualTo(this.request.getContextPath() + "/");
	}

	@Test
	void onNewSessionCustomCookieName() {
		setCookieName("CUSTOM");
		this.strategy.setSessionId(this.request, this.response, this.session.getId());
		assertThat(getSessionId()).isEqualTo(this.session.getId());
	}

	@Test
	void onDeleteSession() {
		this.strategy.expireSession(this.request, this.response);
		assertThat(getSessionId()).isEmpty();
	}

	@Test
	void onDeleteSessionCookiePath() {
		this.request.setContextPath("/somethingunique");
		this.strategy.expireSession(this.request, this.response);

		Cookie sessionCookie = this.response.getCookie(this.cookieName);
		assertThat(sessionCookie.getPath()).isEqualTo(this.request.getContextPath() + "/");
	}

	@Test
	void onDeleteSessionCustomCookieName() {
		setCookieName("CUSTOM");
		this.strategy.expireSession(this.request, this.response);
		assertThat(getSessionId()).isEmpty();
	}

	@Test
	void createSessionCookieValue() {
		assertThat(createSessionCookieValue(17))
				.isEqualToIgnoringCase("0 0 1 1 2 2 3 3 4 4 5 5 6 6 7 7 8 8 9 9 a 10 b 11 c 12 d 13 e 14 f 15 10 16");
	}

	private String createSessionCookieValue(long size) {
		StringBuilder sb = new StringBuilder();

		for (long i = 0; i < size; i++) {
			String hex = Long.toHexString(i);
			sb.append(hex);
			sb.append(" ");
			sb.append(i);
			if (i < size - 1) {
				sb.append(" ");
			}
		}

		return sb.toString();
	}

	private void setCookieName(String cookieName) {
		DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
		cookieSerializer.setCookieName(cookieName);
		this.strategy.setCookieSerializer(cookieSerializer);
		this.cookieName = cookieName;
	}

	private void setSessionCookie(String value) {
		this.request.setCookies(new Cookie(this.cookieName, base64Encode(value)));
	}

	private String getSessionId() {
		return base64Decode(this.response.getCookie(this.cookieName).getValue());
	}

	private static String base64Encode(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}

	private static String base64Decode(String value) {
		return new String(Base64.getDecoder().decode(value));
	}

}
