package org.graylog.plugins.views.search.rest.scriptingapi.response.decorators;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Test;

class IdDecoratorTest {

    @Test
    void accept() {
        final FieldDecorator decorator = new IdDecorator();
        Assertions.assertThat(decorator.accept(RequestedField.parse("streams.id"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input.id"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_node.id"))).isTrue();

        // default is to decorate as title, we don't want IDs
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_node"))).isFalse();
        // unsupported fields are also ignored
        Assertions.assertThat(decorator.accept(RequestedField.parse("http_response_code"))).isFalse();
    }

    @Test
    void decorate() {
        final FieldDecorator decorator = new IdDecorator();
        final SearchUser searchUser = TestSearchUser.builder().build();
        Assertions.assertThat(decorator.decorate(RequestedField.parse("streams.id"), "123", searchUser))
                .isEqualTo("123");
    }
}
