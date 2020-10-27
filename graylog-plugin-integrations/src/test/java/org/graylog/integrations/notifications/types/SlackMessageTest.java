package org.graylog.integrations.notifications.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;


public class SlackMessageTest {


    @Test
    public void test_good_usename() throws IOException {
        SlackMessage message = new SlackMessage("#FF2052", ":turtle:", "https://media.defcon.org/DEF CON 1/DEF CON 1 logo.jpg", "aaa", "#general", false, "this is a happy message", "This is a happy custom message");
        String expected = message.getJsonString();
        List<String> username = getJsonNodeFieldValue(expected,"username");
        assertThat(username).isNotEmpty();
        assertThat(username).isNotNull();
    }

    @Test
    public void test_empty_usernames() throws  IOException{
        SlackMessage message = new SlackMessage("#FF2052",":turtle:","https://media.defcon.org/DEF CON 1/DEF CON 1 logo.jpg",null,"aaa",false,"sss","sss");
        String anotherMessage = message.getJsonString();
        List<String> userNames = getJsonNodeFieldValue(anotherMessage,"username");
        assertThat(userNames).isEmpty();
        assertThat(userNames).isNotNull();

    }

    List<String> getJsonNodeFieldValue(String expected,String fieldName) throws IOException {
        final byte[] bytes = expected.getBytes();
        JsonNode jsonNode = new ObjectMapper().readTree(bytes);
        return jsonNode.findValuesAsText(fieldName);
    }


}