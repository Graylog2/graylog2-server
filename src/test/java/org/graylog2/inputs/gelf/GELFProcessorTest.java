/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.inputs.gelf;

import org.graylog2.gelf.GELFProcessor;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.plugin.Tools;
import org.graylog2.GraylogServerStub;
import org.graylog2.TestHelper;
import org.graylog2.plugin.logmessage.LogMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class GELFProcessorTest {

    public final static double usedTimestamp = Tools.getUTCTimestampWithMilliseconds();
    public final static String GELF_JSON_COMPLETE = "{\"short_message\":\"foo\",\"full_message\":\"foo\\nzomg\",\"facility\":"
            + "\"test\",\"level\":3,\"line\":23,\"file\":\"lol.js\",\"host\":\"bar\",\"timestamp\": " + usedTimestamp + ",\"_lol_utf8\":\"ü\",\"_foo\":\"bar\"}";

    public final static String GELF_JSON_INCOMPLETE = "{\"short_message\":\"foo\",\"host\":\"bar\"}";

    public final static String GELF_JSON_INCOMPLETE_WITH_ID = "{\"short_message\":\"foo\",\"host\":\"bar\",\"_id\":\":7\",\"_something\":\"foo\"}";

    public final static String GELF_JSON_INCOMPLETE_WITH_NON_STANDARD_FIELD = "{\"short_message\":\"foo\",\"host\":\"bar\",\"lol_not_allowed\":\":7\",\"_something\":\"foo\"}";

    public final static String GELF_JSON_WITH_MAP = "{\"short_message\":\"foo\",\"host\":\"bar\",\"_lol\":{\"foo\":\"zomg\",\"bar\":\"baz\"}}";
    
    public final static String GELF_JSON_WITH_ARRAY = "{\"short_message\":\"foo\",\"host\":\"bar\",\"_lol\":[\"foo\",\"zomg\",\"bar\",\"baz\"]}";
 
    public final static String GELF_JSON_WITH_MAP_AND_ARRAY = "{\"short_message\":\"foo\",\"host\":\"bar\",\"_lol\":{\"values\":[\"foo\",\"zomg\",\"bar\",\"baz\",\"\\\"bad\\n\\t\"]}}";
    
    public final static String GELF_JSON_LARGE = "{\"@source\":\"tcp://127.0.0.1:42867/\", \"@tags\":[], \"@fields\":{\"date\":\"2013-02-27T10:37:00.129-06:00\", \"facility\":16, \"facilityName\":\"LOCAL0\", \"severity\":4, \"severityName\":\"WARN\", \"host\":\"web-alpha-nitrogen\", \"application\":\"Example\", \"messageId\":\"application\", \"mdc-Method\":\"GET\", \"mdc-requestId\":\"a48a5302-4cb6-4df4-aac9-e7efb0fed260\", \"mdc-Content-Type\":\"application/x-www-form-urlencoded\", \"mdc-URL\":\"https://example.com/some/url/\", \"mdc-User\":\"administrator\", \"mdc-User-Agent\":\"Jakarta Commons-HttpClient/3.1\", \"mdc-Content-Length\":\"-1\", \"mdc-Locale\":\"en_US\", \"mdc-Organization\":\"example\"}, \"@timestamp\":\"2013-02-27T16:37:00.129Z\", \"@source_host\":\"10.68.77.182\", \"@source_path\":\"/\", \"@message\":\"An unhandled exception occurred in a REST service\\n\\nOrganization: example\\nLogin: \\nUser: administrator\\nEmulating: null\\nLocale: en_US\\nHTTP User-Agent: Jakarta Commons-HttpClient/3.1\\nDetected User-Agent: null null 3.1\\nContent-Type: application/x-www-form-urlencoded\\nContent-Length: -1\\nURL: https://example.com/some/path\\nMethod: GET\\nQuery: null\\n\\ntripod.query.QueryParameterException: Invalid QueryParameter Values specified: 388/USERNAME/OWNER/[cbutl020]\\n\\tat tripod.query.parameter.QueryParameterServiceImpl.validateQueryParameters(QueryParameterServiceImpl.java:271)\\n\\tat tripod.service.v4.data.SchemaDataQueryServiceImpl.buildDataDocument(SchemaDataQueryServiceImpl.java:82)\\n\\tat tripod.service.v4.data.SchemaDataQueryResource.buildDocument(SchemaDataQueryResource.java:142)\\n\\tat tripod.service.v4.data.SchemaDataQueryResource.process(SchemaDataQueryResource.java:122)\\n\\tat tripod.web.rest.BaseDocumentResource.respond(BaseDocumentResource.java:77)\\n\\tat tripod.web.rest.BaseRestServlet.handle(BaseRestServlet.java:121)\\n\\tat tripod.web.rest.BaseRestServlet.doGet(BaseRestServlet.java:215)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:617)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:717)\\n\\tat tripod.web.spring.SpringServletWrapper.handleRequestInternal(SpringServletWrapper.java:77)\\n\\tat org.springframework.web.servlet.mvc.AbstractController.handleRequest(AbstractController.java:153)\\n\\tat org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter.handle(SimpleControllerHandlerAdapter.java:48)\\n\\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:790)\\n\\tat org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:719)\\n\\tat org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:644)\\n\\tat org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:549)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:617)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:717)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:290)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.RequestIdentifierHackFilter.doFilter(RequestIdentifierHackFilter.java:57)\\n\\tat tripod.web.filter.BaseHttpFilter.doFilter(BaseHttpFilter.java:33)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:163)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:237)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:167)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:77)\\n\\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:76)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.LoginRedirectFilter.doFilter(LoginRedirectFilter.java:104)\\n\\tat tripod.web.filter.BaseHttpFilter.doFilter(BaseHttpFilter.java:33)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.RequestWrapperFilter.doFilter(RequestWrapperFilter.java:106)\\n\\tat tripod.web.filter.BaseHttpFilter.doFilter(BaseHttpFilter.java:33)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.CleanupFilter.doFilter(CleanupFilter.java:41)\\n\\tat tripod.web.filter.BaseHttpFilter.doFilter(BaseHttpFilter.java:33)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.ContentTypeHackFilter.doFilter(ContentTypeHackFilter.java:51)\\n\\tat tripod.web.filter.BaseHttpFilter.doFilter(BaseHttpFilter.java:33)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.mdc.MDCFilter.doFilterInternal(MDCFilter.java:43)\\n\\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:76)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:237)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:167)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.web.filter.CustomScopeFilter.doFilter(CustomScopeFilter.java:37)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:237)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:167)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat tripod.servlet.http.HttpSessionWrapperFilter.doFilterInternal(HttpSessionWrapperFilter.java:34)\\n\\tat org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:76)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:237)\\n\\tat org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:167)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:235)\\n\\tat org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:206)\\n\\tat org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:219)\\n\\tat org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:191)\\n\\tat com.digitalmeasures.CookieLoadingValve.invoke(CookieLoadingValve.java:116)\\n\\tat org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:563)\\n\\tat org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:127)\\n\\tat org.apache.catalina.ha.tcp.ReplicationValve.invoke(ReplicationValve.java:347)\\n\\tat org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:102)\\n\\tat org.apache.catalina.valves.RemoteIpValve.invoke(RemoteIpValve.java:637)\\n\\tat org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:109)\\n\\tat com.digitalmeasures.web.metrics.request.tomcat.RequestMetricsValve.invoke(RequestMetricsValve.java:71)\\n\\tat org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:298)\\n\\tat org.apache.coyote.ajp.AjpAprProcessor.process(AjpAprProcessor.java:427)\\n\\tat org.apache.coyote.ajp.AjpAprProtocol$AjpConnectionHandler.process(AjpAprProtocol.java:384)\\n\\tat org.apache.tomcat.util.net.AprEndpoint$Worker.run(AprEndpoint.java:1584)\\n\\tat java.lang.Thread.run(Thread.java:662)\\n\", \"@type\":\"json\"}";

    
    @Test
    public void testMessageReceived() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_COMPLETE)));
        processor.messageReceived(new GELFMessage(TestHelper.gzipCompress(GELF_JSON_COMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;
        
        assertEquals(2, serverStub.callsToProcessBufferInserter);
        assertEquals("foo", lm.getShortMessage());
        assertEquals("foo\nzomg", lm.getFullMessage());
        assertEquals("test", lm.getFacility());
        assertEquals(3, lm.getLevel());
        assertEquals("bar", lm.getHost());
        assertEquals("lol.js", lm.getFile());
        assertEquals(23, lm.getLine());
        assertEquals(usedTimestamp, lm.getCreatedAt(), 1e-8);
        assertEquals("ü", lm.getAdditionalData().get("_lol_utf8"));
        assertEquals("bar", lm.getAdditionalData().get("_foo"));
        assertEquals(2, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedSetsCreatedAtToNowIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(Tools.getUTCTimestampWithMilliseconds(), lm.getCreatedAt(), 2);
    }

    @Test
    public void testMessageReceivedSetsLevelToDefaultIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(LogMessage.STANDARD_LEVEL, lm.getLevel());
    }

    @Test
    public void testMessageReceivedSetsFacilityToDefaultIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(LogMessage.STANDARD_FACILITY, lm.getFacility());
    }

    @Test
    public void testMessageReceivedSkipsSettingIDField() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE_WITH_ID)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertNull(lm.getAdditionalData().get("_id"));
        assertEquals("foo", lm.getAdditionalData().get("_something"));
        assertEquals(1, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedSkipsNonStandardFields() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE_WITH_NON_STANDARD_FIELD)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertNull(lm.getAdditionalData().get("lol_not_allowed"));
        assertEquals("foo", lm.getAdditionalData().get("_something"));
        assertEquals(1, lm.getAdditionalData().size());
    }
    
    @Test
    public void testMessageReceivedConvertsMapsToString() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_WITH_MAP)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals("{\"foo\":\"zomg\",\"bar\":\"baz\"}", lm.getAdditionalData().get("_lol"));
        assertEquals(1, lm.getAdditionalData().size());
    }
    @Test
    public void testMessageReceivedConvertsArraysToString() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_WITH_ARRAY)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals("[\"foo\",\"zomg\",\"bar\",\"baz\"]", lm.getAdditionalData().get("_lol"));
        assertEquals(1, lm.getAdditionalData().size());
    }
    @Test
    public void testMessageReceivedConvertsNestedStructuresToString() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_WITH_MAP_AND_ARRAY)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals("{\"values\":[\"foo\",\"zomg\",\"bar\",\"baz\",\"\\\"bad\\n\\t\"]}", lm.getAdditionalData().get("_lol"));
        assertEquals(1, lm.getAdditionalData().size());
    }
    
    @Test
    public void testLargeMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_LARGE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;
    }
}