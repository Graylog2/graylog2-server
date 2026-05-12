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

import { InputList } from 'components/common';
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
  const suggestions = (data?.tags ?? []).filter((s: string) => !tags.includes(s));

  // Commit every typed tag (only normalize + dedupe + dropping blanks). Invalid values
  // and over-length values are kept so the user can see them as chips, with the offending
  // chips visually marked invalid. Save is blocked by the parent form's validation, which
  // surfaces the server-side rule violations the same way other fields work.
  const handleChange = (event: React.ChangeEvent<{ value: (string | number)[] }>) => {
    const raw = event.target.value as (string | number)[];
    const normalized = raw
      .map((value) => (typeof value === 'string' ? value : String(value)))
      .map(normalizeTag)
      .filter((value) => value.length > 0);
    const deduped = Array.from(new Set(normalized));

    onChange(deduped.slice(0, MAX_TAGS));
  };

  // Duplicates: react-select's default behavior silently refuses to add a tag that's already
  // in the list, leaving the typed text in the input so the user can adjust it. We don't try
  // to surface a validation message for duplicates because the data model is a Set — there's
  // no way to render the conflict as a chip alongside the original, and managing a
  // parallel "duplicate-attempted" state alongside the real tags state created edge cases
  // that weren't worth the marginal feedback gain. The existing chip with the same value is
  // itself the implicit feedback.
  const invalidTags = tags.filter(isInvalidTag);
  const invalidValues = new Set<string | number>(invalidTags);
  const localValidationError = buildInvalidTagsMessage(invalidTags);

  return (
    <InputList
      name="tags"
      id="event-definition-tags"
      values={[...tags]}
      onChange={handleChange}
      placeholder="e.g. authentication, brute-force, compliance"
      help={HELP_TEXT}
      error={error ?? localValidationError}
      invalidValues={invalidValues}
      isClearable
      isDisabled={disabled}
      suggestions={disabled ? undefined : suggestions}
      onSuggestionsInputChange={setInput}
      isLoadingSuggestions={isFetching}
    />
  );
};

export default TagsEditor;
