package org.graylog.integrations.dbconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorCodecTest {
    // Code Under Test
    DBConnectorCodec cut;

    // Mock Objects
    @Mock
    Configuration mockConfiguration;

    // Test Objects
    ObjectMapper mapper = new ObjectMapper();
    RawMessage rawMessage;
    Message message;

    private final MessageFactory messageFactory = new TestMessageFactory();

    // Set up and tear down
    @Before
    public void setUp() {
        cut = new DBConnectorCodec(mockConfiguration, mapper, messageFactory);
    }

    // Test Cases
    @Test(expected = NullPointerException.class)
    public void decode_throwsException_whenNullMessageProvided() throws MisfireException {
        givenNullRawMessage();

        whenDecodeIsCalled();
    }

    @Test
    public void decode_returnsSignInLog() throws Exception {
        givenLogs();

        whenDecodeIsCalled();

        thenMessageFieldIs("app", "contenttypes");

    }

    @Test
    public void decode_storesFullMessage_whenStoreFullMessageConfigFlagSet() throws Exception {
        givenLogs();
        givenStoreFullMessage(true);

        whenDecodeIsCalled();

        thenFullMessageIsStored();
    }

    @Test
    public void decode_doesNotStoreFullMessage_whenStoreFullMessageConfigFalse() throws Exception {
        givenLogs();
        givenStoreFullMessage(false);

        whenDecodeIsCalled();

        thenFullMessageNotStored();
    }

    @Test
    public void testPostgresParsing() {
        given(mockConfiguration.getString(DBConnectorInput.CK_TABLE_NAME)).willReturn("users");
        String connStr = "jdbc:postgresql://192.168.40.222:5432/mydatabase?user=nithin&password=Loginsoft@123";
        String result = cut.getSourceFromConnectionString(connStr);
        assertEquals("192.168.40.222|mydatabase|users", result);
    }

    @Test
    public void testSqlServerParsing() {
        given(mockConfiguration.getString(DBConnectorInput.CK_TABLE_NAME)).willReturn("users");
        String connStr = "jdbc:sqlserver://192.168.40.222:1433;databaseName=master;user=SA;password=Loginsoft@123";
        String result = cut.getSourceFromConnectionString(connStr);
        assertEquals("192.168.40.222|master|users", result);
    }

    @Test
    public void testDb2Parsing() {
        given(mockConfiguration.getString(DBConnectorInput.CK_TABLE_NAME)).willReturn("users");
        String connStr = "jdbc:db2://192.168.40.222:50000/TESTDB:user=db2inst1;password=Loginsoft@123;";
        String result = cut.getSourceFromConnectionString(connStr);
        assertEquals("192.168.40.222|TESTDB|users", result);
    }

    @Test
    public void testMongoParsing() {
        given(mockConfiguration.getString(DBConnectorInput.CK_MONGO_COLLECTION_NAME)).willReturn("users");
        String connStr = "mongodb://user:pass@localhost:27017/mydb";
        String result = cut.getSourceFromConnectionString(connStr);
        assertEquals("localhost|mydb|users", result);
    }

    // GIVENs
    private void givenNullRawMessage() {
        rawMessage = null;
    }

    private void givenLogs()  {
        rawMessage = new RawMessage("{\"_id\": {\"$oid\": \"6061d63407838e1803685a09\"}, \"id\": 1, \"app\": \"contenttypes\", \"name\": \"0001_initial\", \"applied\": {\"$date\": 1617024564527}}".getBytes(StandardCharsets.UTF_8));
        NodeId nodeId = mock(NodeId.class);
        given(nodeId.getNodeId()).willReturn("test-node-id");
        rawMessage.addSourceNode("InputId", nodeId);

    }
    private void givenStoreFullMessage(boolean storeFull) {
        given(mockConfiguration.getBoolean(DBConnectorInput.CK_STORE_FULL_MESSAGE)).willReturn(storeFull);
    }

    // WHENs
    private void whenDecodeIsCalled() {
        message = cut.decodeSafe(rawMessage).get();
    }

    // THENs
    private void thenMessageFieldIs(String field, String value) {
        assertThat(message.getFieldAs(String.class, field), is(value));
    }


    private void thenFullMessageIsStored() {
        assertThat(message.getField("full_message"), notNullValue());
    }

    private void thenFullMessageNotStored() {
        assertThat(message.getField("full_message"), nullValue());
    }


}
