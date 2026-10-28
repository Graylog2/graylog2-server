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
import * as React from 'react';
import { useState } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { EventsDefinitions } from '@graylog/server-api';

import i18n from 'i18n';
import { FormGroup } from 'components/bootstrap';
import { InputDescription, Select, useChipInput } from 'components/common';
import useDebouncedValue from 'hooks/useDebouncedValue';

// Mirror of TagNormalizer on the server. Keep in sync.
const normalizeTag = (raw: string): string => raw.trim().toLowerCase();

// Mirror of EventDefinitionDto.MAX_TAGS / MAX_TAG_LENGTH. Keep in sync.
const MAX_TAGS = 64;
const MAX_TAG_LENGTH = 128;

// Mirror of TagNormalizer.VALID_TAG_PATTERN. Keep in sync.
const VALID_TAG_PATTERN = /^[a-z0-9_.-]+$/;

const SUGGESTION_LIMIT = 50;
const DEBOUNCE_MS = 300;

type Props = {
  tags: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
  error?: React.ReactNode;
};


const isTooLong = (tag: string): boolean => tag.length > MAX_TAG_LENGTH;
const hasInvalidChars = (tag: string): boolean => !VALID_TAG_PATTERN.test(tag);

const quote = (s: string): string => `"${s}"`;

const buildTooLongMessage = (tagsList: ReadonlyArray<string>): string | null => {
  if (tagsList.length === 0) return null;

  return i18n.t('tags_editor.too_long', {
    ns: 'event_definitions',
    count: tagsList.length,
    tags: tagsList.map(quote).join(', '),
    max: MAX_TAG_LENGTH,
  });
};

const buildInvalidCharsMessage = (tagsList: ReadonlyArray<string>): string | null => {
  if (tagsList.length === 0) return null;

  return i18n.t('tags_editor.invalid_characters', {
    ns: 'event_definitions',
    count: tagsList.length,
    tags: tagsList.map(quote).join(', '),
  });
};

const validateTag = (tag: string): string | null => {
  if (isTooLong(tag)) return buildTooLongMessage([tag]);
  if (hasInvalidChars(tag)) return buildInvalidCharsMessage([tag]);

  return null;
};

const TagsEditor = ({ tags, onChange, disabled = false, error = null }: Props) => {
  const { t } = useTranslation('event_definitions');
  // Mirrors the typed text so suggestions are built before the hook call that needs them.
  const [query, setQuery] = useState('');
  const [debouncedQuery] = useDebouncedValue(query, DEBOUNCE_MS);

  const { data, isFetching } = useQuery({
    queryKey: ['event-definitions', 'tag-suggestions', debouncedQuery],
    queryFn: () => EventsDefinitions.suggestTags(debouncedQuery, SUGGESTION_LIMIT),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    enabled: !disabled,
  });
  const isAtMax = tags.length >= MAX_TAGS;
  // At the cap a pick would be sliced off, so disable rather than swallow it.
  const suggestions = (data?.tags ?? [])
    .filter((s: string) => !tags.includes(s))
    .map((value: string) => ({ value, label: value, disabled: isAtMax }));

  const { messages, flagDuplicateAttempt, selectProps, reactSelectProps } = useChipInput({
    values: tags,
    onChange,
    normalize: normalizeTag,
    validate: validateTag,
    max: MAX_TAGS,
    onInputChanged: setQuery,
    // At most one bucket per tag, so a message names one failure mode.
    describeInvalid: (invalid) =>
      [
        buildTooLongMessage(invalid.filter(isTooLong)),
        buildInvalidCharsMessage(invalid.filter((tag) => !isTooLong(tag) && hasInvalidChars(tag))),
      ]
        .filter(Boolean)
        .join(' ') || null,
    describeDuplicate: (tag) => t('tags_editor.duplicate', { tag }),
    // Already offered as a suggestion, so a Create row would duplicate it.
    suppressCreate: (_raw, candidate) => suggestions.some((option) => option.value === candidate),
  });

  const allMessages = [...messages, isAtMax ? t('tags_editor.max_reached', { max: MAX_TAGS }) : null].filter(Boolean);
  const localValidationError =
    allMessages.length === 0 ? null : (
      <>
        {allMessages.map((m) => (
          <div key={m}>{m}</div>
        ))}
      </>
    );
  const combinedError = error ?? localValidationError;

  return (
    <FormGroup controlId="event-definition-tags" validationState={combinedError ? 'error' : null}>
      <Select
        {...selectProps}
        {...reactSelectProps}
        // Not in `Select`'s Props type, so cast to satisfy tsc.
        {...({
          onKeyDown: (e: React.KeyboardEvent) => {
            if (e.key === 'Enter' || e.key === 'Tab') {
              flagDuplicateAttempt();
            }
          },
        } as object)}
        inputId="event-definition-tags"
        aria-label={t('tags_editor.aria_label')}
        multi
        allowCreate
        options={suggestions}
        isLoading={isFetching}
        disabled={disabled}
        placeholder={t('tags_editor.placeholder')}
      />
      <InputDescription error={combinedError} help={t('tags_editor.help')} />
    </FormGroup>
  );
};

export default TagsEditor;
