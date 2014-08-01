package org.graylog2.outputs;

import org.graylog2.GelfMessage;
import org.graylog2.GelfSender;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.Mock;
import static org.testng.Assert.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class GelfOutputTest {
    public void testWrite() throws Exception {
        final GelfSender gelfSender = mock(GelfSender.class);
        final Message message = mock(Message.class);
        final GelfMessage gelfMessage = mock(GelfMessage.class);

        final GelfOutput gelfOutput = Mockito.spy(new GelfOutput());
        doReturn(gelfSender).when(gelfOutput).getGelfSender(any(Configuration.class));
        doReturn(gelfMessage).when(gelfOutput).toGELFMessage(message);

        gelfOutput.write(message);

        verify(gelfSender).sendMessage(eq(gelfMessage));
    }

    public void testGetRequestedConfiguration() throws Exception {
        final GelfOutput gelfOutput = new GelfOutput();

        final ConfigurationRequest request = gelfOutput.getRequestedConfiguration();

        assertNotNull(request);
        assertNotNull(request.asList());
    }
}
