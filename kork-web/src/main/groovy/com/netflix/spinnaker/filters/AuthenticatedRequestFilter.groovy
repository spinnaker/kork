/*
 * Copyright 2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.filters

import com.netflix.spinnaker.kork.common.Header
import com.netflix.spinnaker.security.AllowedAccountAuthority
import com.netflix.spinnaker.security.AuthenticatedRequest
import groovy.util.logging.Slf4j
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.security.cert.X509Certificate

@Slf4j
class AuthenticatedRequestFilter extends OncePerRequestFilter {
  private static final String X509_CERTIFICATE = "javax.servlet.request.X509Certificate"

  /*
    otherName                       [0]
    rfc822Name                      [1]
    dNSName                         [2]
    x400Address                     [3]
    directoryName                   [4]
    ediPartyName                    [5]
    uniformResourceIdentifier       [6]
    iPAddress                       [7]
    registeredID                    [8]
  */
  private static final String RFC822_NAME_ID = "1"

  private final boolean extractSpinnakerHeaders
  private final boolean extractSpinnakerUserOriginHeader
  private final boolean forceNewSpinnakerRequestId
  private final boolean clearAuthenticatedRequestPostFilter
  private final SecurityContextRepository securityContextRepository

  public AuthenticatedRequestFilter(boolean extractSpinnakerHeaders = false,
                                    boolean extractSpinnakerUserOriginHeader = false,
                                    boolean forceNewSpinnakerRequestId = false,
                                    boolean clearAuthenticatedRequestPostFilter = true,
                                    SecurityContextRepository securityContextRepository = null) {
    this.extractSpinnakerHeaders = extractSpinnakerHeaders
    this.extractSpinnakerUserOriginHeader = extractSpinnakerUserOriginHeader
    this.forceNewSpinnakerRequestId = forceNewSpinnakerRequestId
    this.clearAuthenticatedRequestPostFilter = clearAuthenticatedRequestPostFilter
    this.securityContextRepository = securityContextRepository ?: new HttpSessionSecurityContextRepository()
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String spinnakerUser = null
    String spinnakerAccounts = null
    Map<String, String> otherSpinnakerHeaders = [:]

    try {
      SecurityContext securityContext
      // first check if there is a session with a SecurityContext (but don't create the session yet)
      if (securityContextRepository.containsContext(request)) {
        def holder = new HttpRequestResponseHolder(request, response)
        securityContext = securityContextRepository.loadContext(holder)
      } else {
        // otherwise, try checking SecurityContextHolder as this may be a fresh session
        securityContext = SecurityContextHolder.context
      }

      // next, if an authenticated user is present, get their username and allowed account authorities
      // (when using OAuth2, the principal may be an OAuth2User or OidcUser rather than a UserDetails instance)
      def auth = securityContext.authentication
      if (auth && auth.authenticated) {
        spinnakerUser = auth.name
        spinnakerAccounts = auth.authorities
          .grep(AllowedAccountAuthority)
          .collect { (it as AllowedAccountAuthority).account }
          .join(',')
      }
    } catch (Exception e) {
      log.error("Unable to extract spinnaker user and account information", e)
    }

    if (extractSpinnakerHeaders) {
      // for backend services using the x-spinnaker-user header for authentication
      spinnakerUser = spinnakerUser ?: request.getHeader(Header.USER.header)
      spinnakerAccounts = spinnakerAccounts ?: request.getHeader(Header.ACCOUNTS.header)

      for (header in request.headerNames) {
        String headerUpper = header.toUpperCase()

        if (headerUpper.startsWith(Header.XSpinnakerPrefix)) {
          otherSpinnakerHeaders[headerUpper] = request.getHeader(header)
        }
      }
    }

    // normalize anonymous principal name in case of misconfiguration
    // [anonymous] => anonymous (occasional use in Kayenta, potentially used in Orca; shows up in test data)
    // anonymousUser => anonymous (default principal in Spring Security's AnonymousAuthenticationFilter)
    // __unrestricted_user__ => anonymous (principal used in Fiat for anonymous)
    if (spinnakerUser ==~ /\[?anonymous]?User|__unrestricted_user__/) {
      spinnakerUser = 'anonymous'
    }

    if (extractSpinnakerUserOriginHeader) {
      def app = request.getHeader('X-RateLimit-App')
      def origin = 'deck'.equalsIgnoreCase(app) ? 'deck' : 'api'
      otherSpinnakerHeaders[Header.USER_ORIGIN.header] = origin
    }

    if (forceNewSpinnakerRequestId) {
      otherSpinnakerHeaders.put(
        Header.REQUEST_ID.getHeader(),
        UUID.randomUUID().toString()
      )
    }

    // only extract from the x509 certificate if `spinnakerUser` has not been supplied as a header
    if (request.isSecure() && !spinnakerUser) {
      ((X509Certificate[]) request.getAttribute(X509_CERTIFICATE))?.each {
        def emailSubjectName = it.getSubjectAlternativeNames().find {
          it.find { it.toString() == RFC822_NAME_ID }
        }?.get(1)

        spinnakerUser = emailSubjectName
      }
    }

    try {
      if (spinnakerUser) {
        AuthenticatedRequest.setUser(spinnakerUser)
      }

      if (spinnakerAccounts) {
        AuthenticatedRequest.setAccounts(spinnakerAccounts)
      }

      for (header in otherSpinnakerHeaders) {
        AuthenticatedRequest.set(header.key, header.value)
      }

      chain.doFilter(request, response)
    } finally {
      if (clearAuthenticatedRequestPostFilter) {
        AuthenticatedRequest.clear()
      }
    }
  }
}
