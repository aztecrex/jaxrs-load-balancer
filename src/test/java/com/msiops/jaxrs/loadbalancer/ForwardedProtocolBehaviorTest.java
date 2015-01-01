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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ForwardedProtocolBehaviorTest {

    @Parameters(name = "{index}: proto={0}  port={1}  have={2}  want={3}")
    public static Collection<Object[]> cases() {

        return Arrays.asList(

        // @formatter:off
        new Object[] {null, null, "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {null, null, "https://happy.com/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", null, "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"https", null, "https://happy.com/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "8080", "http://happy.com:8080/happy/path?happy=birthday&to=you","http://happy.com:8080/happy/path?happy=birthday&to=you" },
        new Object[] {"https", "8443", "https://happy.com:8443/happy/path?happy=birthday&to=you","https://happy.com:8443/happy/path?happy=birthday&to=you" },
        new Object[] {"https", null, "http://happy.com/happy/path?happy=birthday&to=you","https://happy.com:80/happy/path?happy=birthday&to=you" },
        new Object[] {"http", null, "https://happy.com/happy/path?happy=birthday&to=you","http://happy.com:443/happy/path?happy=birthday&to=you" },
        new Object[] {null, "8000", "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com:8000/happy/path?happy=birthday&to=you" },
        new Object[] {null, " 008000 ", "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com:8000/happy/path?happy=birthday&to=you" },
        new Object[] {null, "80", "http://happy.com:8000/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {null, "443", "https://happy.com:8000/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"https", "443", "http://happy.com/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"https", "8443", "http://happy.com/happy/path?happy=birthday&to=you","https://happy.com:8443/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "80", "https://happy.com/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "8080", "https://happy.com/happy/path?happy=birthday&to=you","http://happy.com:8080/happy/path?happy=birthday&to=you" },
        new Object[] {"https", "443", "nothttp://happy.com/happy/path?happy=birthday&to=you","nothttp://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "80", "nothttps://happy.com/happy/path?happy=birthday&to=you","nothttps://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "80", "/happy/path?happy=birthday&to=you","/happy/path?happy=birthday&to=you" },
        new Object[] {"https", "443", "/happy/path?happy=birthday&to=you","/happy/path?happy=birthday&to=you" },
        new Object[] {"https", null, "http:opaque","http:opaque" },
        new Object[] {"http", null, "https:opaque","https:opaque" },
        new Object[] {"https", "undecipherable!", "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"http", "undecipherable!", "https://happy.com/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"nothttp", null, "http://happy.com/happy/path?happy=birthday&to=you","http://happy.com/happy/path?happy=birthday&to=you" },
        new Object[] {"nothttps", null, "https://happy.com/happy/path?happy=birthday&to=you","https://happy.com/happy/path?happy=birthday&to=you" }
        // @formatter:on
                );

    }

    private ContainerRequestContext ctx;

    private final URI expectedFixedUrl;

    private ForwardedProtocolFilter filter;

    private final String forwardedPort;

    private final String forwardedProto;

    private final URI incomingUrl;

    public ForwardedProtocolBehaviorTest(final String fproto,
            final String fport, final String in, final String fixed) {

        this.forwardedProto = fproto;
        this.forwardedPort = fport;

        this.incomingUrl = URI.create(in);
        this.expectedFixedUrl = URI.create(fixed);

    }

    @Before
    public void setup() {
        this.filter = new ForwardedProtocolFilter();

        final UriInfo uriIn = mock(UriInfo.class);
        when(uriIn.getRequestUri()).thenReturn(this.incomingUrl);

        this.ctx = mock(ContainerRequestContext.class);
        when(this.ctx.getUriInfo()).thenReturn(uriIn);
        when(this.ctx.getHeaderString("x-forwarded-proto")).thenReturn(
                this.forwardedProto);
        when(this.ctx.getHeaderString("x-forwarded-port")).thenReturn(
                this.forwardedPort);

    }

    @Test
    public void testBehavior() throws IOException {

        /*
         * when filter is invoked on context
         */
        this.filter.filter(this.ctx);

        if (this.incomingUrl.equals(this.expectedFixedUrl)) {
            /*
             * when expected uri is the same as the incoming uri
             */
            /*
             * then it should not set a new URI
             */
            verify(this.ctx, never()).setRequestUri(any());
            verify(this.ctx, never()).setRequestUri(any(), any());
        } else {
            /*
             * when expected uri is not the same as the incoming uri
             */
            /*
             * then it should set the request URI to the expected value
             */
            verify(this.ctx).setRequestUri(this.expectedFixedUrl);
        }

    }
}
