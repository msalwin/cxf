/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.security;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.https.CertificateHostnameVerifier;
import org.apache.cxf.transport.https.SSLUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRS20HttpsBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookHttpsServer.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookHttpsServer.class, true));
    }

    @Test
    public void testGetBook() throws Exception {
        
        ClientBuilder builder = ClientBuilder.newBuilder();
        
        KeyStore trustStore = loadStore("src/test/java/org/apache/cxf/systest/http/resources/Truststore.jks",
                                       "password");
        
        builder.trustStore(trustStore);
        builder.hostnameVerifier(CertificateHostnameVerifier.ALLOW_ALL);
        
        KeyStore keyStore = loadStore("src/test/java/org/apache/cxf/systest/http/resources/Morpit.jks",
            "password");
        builder.keyStore(keyStore, "password");
        
        Client client = builder.build();
        
        WebTarget target = client.target("https://localhost:" + PORT + "/bookstore/securebooks/123");
        Book b = target.request().accept(MediaType.APPLICATION_XML_TYPE).get(Book.class);
        assertEquals(123, b.getId());
    }
    
    @Test
    public void testGetBookSslContext() throws Exception {
        
        ClientBuilder builder = ClientBuilder.newBuilder();
        
        SSLContext sslContext = createSSLContext();
        builder.sslContext(sslContext);
        
        builder.hostnameVerifier(CertificateHostnameVerifier.ALLOW_ALL);
        
        
        Client client = builder.build();
        
        WebTarget target = client.target("https://localhost:" + PORT + "/bookstore/securebooks/123");
        Book b = target.request().accept(MediaType.APPLICATION_XML_TYPE).get(Book.class);
        assertEquals(123, b.getId());
    }
    
    private KeyStore loadStore(String trustStoreFile, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(trustStoreFile), password.toCharArray());
        return store;
    }
    
    private SSLContext createSSLContext() throws Exception {
        TLSClientParameters tlsParams = new TLSClientParameters();
        
        KeyStore trustStore = loadStore("src/test/java/org/apache/cxf/systest/http/resources/Truststore.jks",
            "password");
        
        TrustManagerFactory tmf = 
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        tlsParams.setTrustManagers(tmf.getTrustManagers());
        
        KeyStore keyStore = loadStore("src/test/java/org/apache/cxf/systest/http/resources/Morpit.jks",
            "password");
        
        KeyManagerFactory kmf = 
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());
        tlsParams.setKeyManagers(kmf.getKeyManagers());
        
        return SSLUtils.getSSLContext(tlsParams);
    }
}
