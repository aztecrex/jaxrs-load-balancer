/**
 * Licensed under the Apache License, Version 2.0 (the "License") under
 * one or more contributor license agreements. See the NOTICE file
 * distributed with this work for information regarding copyright
 * ownership. You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.msiops.jaxrs.loadbalancer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

/**
 * A pre-matching filter that detects load balancer scheme change and sets the
 * incoming scheme accordingly.
 */
@PreMatching()
public class ForwardedProtocolFilter implements ContainerRequestFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {

        final String fproto = requestContext
                .getHeaderString("x-forwarded-proto");
        final String fport = requestContext.getHeaderString("x-forwarded-port");

        final URI in = requestContext.getUriInfo().getRequestUri();

        if (!in.isAbsolute()) {
            /*
             * no point dealing with a non-absolute uri
             */
            return;
        }

        if (in.isOpaque()) {
            /*
             * no point dealing with something definitely not a url
             */
            return;
        }

        final boolean haveHttps;
        if (in.getScheme().equalsIgnoreCase("http")) {
            haveHttps = false;
        } else if (in.getScheme().equalsIgnoreCase("https")) {
            haveHttps = true;
        } else {
            /*
             * not an HTTP(S) request URI, do nothing
             */
            return;
        }

        final boolean wantHttps;
        if (fproto == null) {
            wantHttps = haveHttps;
        } else if (fproto.equalsIgnoreCase("http")) {
            wantHttps = false;
        } else if (fproto.equalsIgnoreCase("https")) {
            wantHttps = true;
        } else {
            /*
             * not an HTTP(S) request URI, do nothing
             */
            return;
        }

        final int havePort;
        if (in.getPort() == -1) {
            havePort = haveHttps ? 443 : 80;
        } else {
            havePort = in.getPort();
        }

        final int wantPort;
        if (fport == null) {
            wantPort = havePort;
        } else {
            try {
                wantPort = Integer.parseInt(fport.trim());
            } catch (final Exception x) {
                // error parsing port, change nothing
                return;
            }
        }

        final String newScheme = haveHttps == wantHttps ? in.getScheme()
                : (wantHttps ? "https" : "http");
        final int stdPort = wantHttps ? 443 : 80;
        final int newPort = wantPort == stdPort ? -1 : wantPort;

        final URI out;
        try {
            out = new URI(newScheme, in.getUserInfo(), in.getHost(), newPort,
                    in.getPath(), in.getQuery(), in.getFragment());
        } catch (final URISyntaxException e) {
            // any problem and we just don't modify the url
            return;
        }
        if (!in.equals(out)) {
            requestContext.setRequestUri(out);
        }

    }
}
