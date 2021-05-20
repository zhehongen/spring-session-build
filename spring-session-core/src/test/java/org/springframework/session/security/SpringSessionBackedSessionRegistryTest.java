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

package org.springframework.session.security;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.User;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link SpringSessionBackedSessionRegistry}.
 */
class SpringSessionBackedSessionRegistryTest {

	private static final String SESSION_ID = "sessionId";

	private static final String SESSION_ID2 = "otherSessionId";

	private static final String USER_NAME = "userName";

	private static final User PRINCIPAL = new User(USER_NAME, "password", Collections.emptyList());

	private static final Instant NOW = Instant.now();

	@Mock
	private FindByIndexNameSessionRepository<Session> sessionRepository;

	@InjectMocks
	private SpringSessionBackedSessionRegistry<Session> sessionRegistry;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void sessionInformationForExistingSession() {
		Session session = createSession(SESSION_ID, USER_NAME, NOW);
		when(this.sessionRepository.findById(SESSION_ID)).thenReturn(session);

		SessionInformation sessionInfo = this.sessionRegistry.getSessionInformation(SESSION_ID);

		assertThat(sessionInfo.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(sessionInfo.getLastRequest().toInstant().truncatedTo(ChronoUnit.MILLIS))
				.isEqualTo(NOW.truncatedTo(ChronoUnit.MILLIS));
		assertThat(sessionInfo.getPrincipal()).isEqualTo(USER_NAME);
		assertThat(sessionInfo.isExpired()).isFalse();
	}

	@Test
	void sessionInformationForExpiredSession() {
		Session session = createSession(SESSION_ID, USER_NAME, NOW);
		session.setAttribute(SpringSessionBackedSessionInformation.EXPIRED_ATTR, Boolean.TRUE);
		when(this.sessionRepository.findById(SESSION_ID)).thenReturn(session);

		SessionInformation sessionInfo = this.sessionRegistry.getSessionInformation(SESSION_ID);

		assertThat(sessionInfo.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(sessionInfo.getLastRequest().toInstant().truncatedTo(ChronoUnit.MILLIS))
				.isEqualTo(NOW.truncatedTo(ChronoUnit.MILLIS));
		assertThat(sessionInfo.getPrincipal()).isEqualTo(USER_NAME);
		assertThat(sessionInfo.isExpired()).isTrue();
	}

	@Test
	void noSessionInformationForMissingSession() {
		assertThat(this.sessionRegistry.getSessionInformation("nonExistingSessionId")).isNull();
	}

	@Test
	void getAllSessionsForUserDetails() {
		setUpSessions();
		List<SessionInformation> allSessionInfos = this.sessionRegistry.getAllSessions(PRINCIPAL, true);
		assertThat(allSessionInfos).extracting("sessionId").containsExactly(SESSION_ID, SESSION_ID2);
	}

	@Test
	void getAllSessionsForAuthenticatedPrincipal() {
		setUpSessions();
		List<SessionInformation> allSessionInfos = this.sessionRegistry
				.getAllSessions((AuthenticatedPrincipal) () -> USER_NAME, true);
		assertThat(allSessionInfos).extracting("sessionId").containsExactly(SESSION_ID, SESSION_ID2);
	}

	@Test
	void getAllSessionsForPrincipal() {
		setUpSessions();
		List<SessionInformation> allSessionInfos = this.sessionRegistry.getAllSessions(new TestPrincipal(USER_NAME),
				true);
		assertThat(allSessionInfos).extracting("sessionId").containsExactly(SESSION_ID, SESSION_ID2);
	}

	@Test
	void getNonExpiredSessions() {
		setUpSessions();

		List<SessionInformation> nonExpiredSessionInfos = this.sessionRegistry.getAllSessions(PRINCIPAL, false);

		assertThat(nonExpiredSessionInfos).extracting("sessionId").containsExactly(SESSION_ID2);
	}

	@Test
	void expireNow() {
		Session session = createSession(SESSION_ID, USER_NAME, NOW);
		when(this.sessionRepository.findById(SESSION_ID)).thenReturn(session);

		SessionInformation sessionInfo = this.sessionRegistry.getSessionInformation(SESSION_ID);
		assertThat(sessionInfo.isExpired()).isFalse();

		sessionInfo.expireNow();

		assertThat(sessionInfo.isExpired()).isTrue();
		ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
		verify(this.sessionRepository).save(captor.capture());
		assertThat(captor.getValue().<Boolean>getAttribute(SpringSessionBackedSessionInformation.EXPIRED_ATTR))
				.isEqualTo(Boolean.TRUE);
	}

	private Session createSession(String sessionId, String userName, Instant lastAccessed) {
		MapSession session = new MapSession(sessionId);
		session.setLastAccessedTime(lastAccessed);
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn(userName);
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(authentication);
		session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
		return session;
	}

	private void setUpSessions() {
		Session session1 = createSession(SESSION_ID, USER_NAME, NOW);
		session1.setAttribute(SpringSessionBackedSessionInformation.EXPIRED_ATTR, Boolean.TRUE);
		Session session2 = createSession(SESSION_ID2, USER_NAME, NOW);
		Map<String, Session> sessions = new LinkedHashMap<>();
		sessions.put(session1.getId(), session1);
		sessions.put(session2.getId(), session2);
		when(this.sessionRepository.findByPrincipalName(USER_NAME)).thenReturn(sessions);
	}

	private static final class TestPrincipal implements Principal {

		private final String name;

		private TestPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object another) {
			if (this == another) {
				return true;
			}
			if (another instanceof TestPrincipal) {
				return this.name.equals(((TestPrincipal) another).name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
