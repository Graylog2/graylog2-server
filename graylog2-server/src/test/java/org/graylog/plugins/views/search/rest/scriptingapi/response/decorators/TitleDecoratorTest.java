package org.graylog.plugins.views.search.rest.scriptingapi.response.decorators;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class TitleDecoratorTest {

    @Test
    void testPermitted() {
        final FieldDecorator decorator = new TitleDecorator((request, permissions) -> EntitiesTitleResponse.EMPTY_RESPONSE);
        Assertions.assertThat(decorator.accept(RequestedField.parse("streams"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("streams.name"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("streams.title"))).isTrue();

        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input.name"))).isTrue();
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input.title"))).isTrue();

        // For IDs we have a different decorator
        Assertions.assertThat(decorator.accept(RequestedField.parse("streams.id"))).isFalse();
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input.id"))).isFalse();
        // unknown decorator
        Assertions.assertThat(decorator.accept(RequestedField.parse("gl2_source_input.uppercase"))).isFalse();
        // other fields and entities are not supported
        Assertions.assertThat(decorator.accept(RequestedField.parse("http_response_code"))).isFalse();
        Assertions.assertThat(decorator.accept(RequestedField.parse("http_response_code.name"))).isFalse();
    }

    @Test
    void testDecorate() {
        final EntitiesTitleResponse response = new EntitiesTitleResponse(Collections.singleton(new EntityTitleResponse("123", "streams", "My stream")), Collections.emptySet());
        final FieldDecorator decorator = new TitleDecorator((request, permissions) -> response);
        Assertions.assertThat(decorator.decorate(RequestedField.parse("streams"), "123", TestSearchUser.builder().build()))
                .isEqualTo("My stream");
    }

    @Test
    void testDecorateNotPermitted() {
        final EntitiesTitleResponse response = new EntitiesTitleResponse(Collections.emptySet(), Collections.singleton("123"));
        final FieldDecorator decorator = new TitleDecorator((request, permissions) -> response);
        Assertions.assertThat(decorator.decorate(RequestedField.parse("streams"), "123", TestSearchUser.builder().build()))
                .isEqualTo("123");
    }


    @Test
    void testMixedPermittedAndNotPermitted() {
        final EntitiesTitleResponse response = new EntitiesTitleResponse(Collections.singleton(new EntityTitleResponse("123", "streams", "My stream")), Collections.singleton("456"));
        final FieldDecorator decorator = new TitleDecorator((request, permissions) -> response);
        final Object result = decorator.decorate(RequestedField.parse("streams"), Arrays.asList("123", "456"), TestSearchUser.builder().build());
        Assertions.assertThat(result)
                .isInstanceOf(List.class)
                .asList()
                .hasSize(2)
                .contains("My stream")
                .contains("456");
    }
}
