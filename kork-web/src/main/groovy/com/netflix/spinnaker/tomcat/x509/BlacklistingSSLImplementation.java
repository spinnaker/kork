/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.tomcat.x509;

import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLUtil;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;

/**
 * An SSLImplementation that enforces a blacklist of client certificates.
 *
 * To enable the blacklist behavior, use the {{AbstractEndpoint}} {{crlFile}} property
 * following the naming convention {{blacklist:/path/to/blacklist/file}}
 *
 * The format of the blacklist file is one entry per line conforming to
 * {{issuer X500 name:::blacklisted certificate serial number}}
 * Blank lines and lines that begin with {{#}} are ignored.
 *
 * The Blacklist file is refreshed periodically, so it can be updated in place to pick up newly
 * revoked certificates.
 */
public class BlacklistingSSLImplementation extends JSSEImplementation {

  @Override
  public SSLUtil getSSLUtil(SSLHostConfigCertificate certificate) {
    return new BlacklistingJSSESocketFactory(certificate);
  }
}
