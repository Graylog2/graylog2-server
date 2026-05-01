/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.tags;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    private static final String TAG_ID = "tag-id";
    private static final String OLD_VALUE = "phishing";
    private static final String NEW_VALUE = "credential-theft";

    @Mock
    private DBTagService dbTagService;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(dbTagService);
    }

    @Test
    void createPersistsNewTag() {
        when(dbTagService.getByValue(NEW_VALUE)).thenReturn(Optional.empty());
        final Tag built = Tag.builder().value(NEW_VALUE).build();
        final Tag saved = built.toBuilder().id(TAG_ID).build();
        when(dbTagService.save(built)).thenReturn(saved);

        final Tag result = tagService.create(NEW_VALUE);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createRejectsDuplicateValue() {
        final Tag existing = Tag.builder().id(TAG_ID).value(NEW_VALUE).build();
        when(dbTagService.getByValue(NEW_VALUE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tagService.create(NEW_VALUE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(NEW_VALUE);

        verify(dbTagService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRenamesTag() {
        final Tag existing = Tag.builder().id(TAG_ID).value(OLD_VALUE).build();
        final Tag updated = existing.toBuilder().value(NEW_VALUE).build();

        when(dbTagService.get(TAG_ID)).thenReturn(Optional.of(existing));
        when(dbTagService.getByValue(NEW_VALUE)).thenReturn(Optional.empty());
        when(dbTagService.update(TAG_ID, NEW_VALUE)).thenReturn(updated);

        assertThat(tagService.update(TAG_ID, NEW_VALUE)).isEqualTo(updated);
    }

    @Test
    void updateNoOpWhenSameValue() {
        final Tag existing = Tag.builder().id(TAG_ID).value(OLD_VALUE).build();

        when(dbTagService.get(TAG_ID)).thenReturn(Optional.of(existing));
        when(dbTagService.getByValue(OLD_VALUE)).thenReturn(Optional.of(existing));

        assertThat(tagService.update(TAG_ID, OLD_VALUE)).isEqualTo(existing);
        verify(dbTagService, never()).update(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateRejectsRenameToExistingValue() {
        final Tag existing = Tag.builder().id(TAG_ID).value(OLD_VALUE).build();
        final Tag conflict = Tag.builder().id("other-id").value(NEW_VALUE).build();

        when(dbTagService.get(TAG_ID)).thenReturn(Optional.of(existing));
        when(dbTagService.getByValue(NEW_VALUE)).thenReturn(Optional.of(conflict));

        assertThatThrownBy(() -> tagService.update(TAG_ID, NEW_VALUE))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateThrowsWhenNotFound() {
        when(dbTagService.get(TAG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.update(TAG_ID, NEW_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteReturnsRemovedTag() {
        final Tag existing = Tag.builder().id(TAG_ID).value(OLD_VALUE).build();

        when(dbTagService.get(TAG_ID)).thenReturn(Optional.of(existing));
        when(dbTagService.delete(TAG_ID)).thenReturn(1L);

        assertThat(tagService.delete(TAG_ID)).isEqualTo(existing);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(dbTagService.get(TAG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.delete(TAG_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
