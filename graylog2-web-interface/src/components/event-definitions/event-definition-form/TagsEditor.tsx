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

import { EventsDefinitions } from '@graylog/server-api';

import { FormGroup } from 'components/bootstrap';
import { InputDescription, Select } from 'components/common';
import useDebouncedValue from 'hooks/useDebouncedValue';

// Mirror of TagNormalizer on the server. Keep in sync.
const normalizeTag = (raw: string): string => raw.trim().toLowerCase();

// Mirror of EventDefinitionDto.MAX_TAGS / MAX_TAG_LENGTH. Keep in sync.
const MAX_TAGS = 64;
const MAX_TAG_LENGTH = 128;

// Mirror of TagNormalizer.VALID_TAG_PATTERN. Keep in sync.
const VALID_TAG_PATTERN = /^[a-z0-9_-]+$/;

const SUGGESTION_LIMIT = 50;
const DEBOUNCE_MS = 300;

// Unit-separator character; can't appear in a tag value (validation restricts tags to
// [a-z0-9_-]) so it's safe to use as the delimiter Select uses to (de)serialize multi values.
const VALUE_DELIMITER = '\x1F';

type Props = {
  tags: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
  error?: React.ReactNode;
};

const HELP_TEXT = 'Press Enter or Tab to add. Tags are lowercased and deduplicated. Only lowercase letters, digits, hyphens, and underscores are allowed.';

const isInvalidTag = (tag: string): boolean =>
  tag.length > MAX_TAG_LENGTH || !VALID_TAG_PATTERN.test(tag);

const quote = (s: string): string => `"${s}"`;

const buildInvalidTagsMessage = (invalidTags: ReadonlyArray<string>): string | null => {
  if (invalidTags.length === 0) return null;

  const isPlural = invalidTags.length > 1;

  return `Tag${isPlural ? 's' : ''} ${invalidTags.map(quote).join(', ')} ${isPlural ? 'are' : 'is'} invalid. `
    + `Tags may only contain lowercase letters, digits, hyphens and underscores (max ${MAX_TAG_LENGTH} chars each).`;
};

const TagsEditor = ({ tags, onChange, disabled = false, error = null }: Props) => {
  const [input, setInput] = useState('');
  const [debouncedInput] = useDebouncedValue(input, DEBOUNCE_MS);

  const { data, isFetching } = useQuery({
    queryKey: ['event-definitions', 'tag-suggestions', debouncedInput],
    queryFn: () => EventsDefinitions.suggestTags(debouncedInput, SUGGESTION_LIMIT),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    enabled: !disabled,
  });
  const suggestions = (data?.tags ?? [])
    .filter((s: string) => !tags.includes(s))
    .map((value: string) => ({ value, label: value }));

  const handleChange = (joined: string) => {
    const raw = joined ? joined.split(VALUE_DELIMITER) : [];
    const normalized = raw.map(normalizeTag).filter((value) => value.length > 0);
    const deduped = Array.from(new Set(normalized));

    onChange(deduped.slice(0, MAX_TAGS));
  };

  const invalidTags = tags.filter(isInvalidTag);
  const localValidationError = buildInvalidTagsMessage(invalidTags);
  const combinedError = error ?? localValidationError;

  return (
    <FormGroup controlId="event-definition-tags" validationState={combinedError ? 'error' : null}>
      <Select
        inputId="event-definition-tags"
        multi
        allowCreate
        delimiter={VALUE_DELIMITER}
        options={suggestions}
        value={tags.join(VALUE_DELIMITER)}
        onChange={handleChange}
        onInputChange={(value) => setInput(value)}
        isLoading={isFetching}
        disabled={disabled}
        placeholder="e.g. authentication, brute-force, compliance"
      />
      <InputDescription error={combinedError} help={HELP_TEXT} />
    </FormGroup>
  );
};

export default TagsEditor;
