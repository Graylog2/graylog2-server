package org.graylog2.restclient.models.api.requests;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class ChangePasswordRequestTest {
    @Test
    public void testToJson() {
        final ChangePasswordRequest request = new ChangePasswordRequest();
        request.setPassword("password");
        request.setOld_password("old_password");
        final String json = request.toJson();

        assertNotNull(json);
    }

}