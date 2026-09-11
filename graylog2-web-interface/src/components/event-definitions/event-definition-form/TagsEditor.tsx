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

const HELP_TEXT =
  'Press Enter or Tab to add. Tags are lowercased and deduplicated. Only lowercase letters, digits, hyphens, underscores, and dots are allowed.';

const isTooLong = (tag: string): boolean => tag.length > MAX_TAG_LENGTH;
const hasInvalidChars = (tag: string): boolean => !VALID_TAG_PATTERN.test(tag);

const quote = (s: string): string => `"${s}"`;

const buildTooLongMessage = (tagsList: ReadonlyArray<string>): string | null => {
  if (tagsList.length === 0) return null;

  const isPlural = tagsList.length > 1;

  return (
    `Tag${isPlural ? 's' : ''} ${tagsList.map(quote).join(', ')} ` +
    `${isPlural ? 'exceed' : 'exceeds'} the maximum length of ${MAX_TAG_LENGTH} characters.`
  );
};

const buildInvalidCharsMessage = (tagsList: ReadonlyArray<string>): string | null => {
  if (tagsList.length === 0) return null;

  const isPlural = tagsList.length > 1;

  return (
    `Tag${isPlural ? 's' : ''} ${tagsList.map(quote).join(', ')} ` +
    `${isPlural ? 'contain' : 'contains'} invalid characters. ` +
    'Only lowercase letters, digits, hyphens, underscores, and dots are allowed.'
  );
};

const validateTag = (tag: string): string | null => {
  if (isTooLong(tag)) return buildTooLongMessage([tag]);
  if (hasInvalidChars(tag)) return buildInvalidCharsMessage([tag]);

  return null;
};

const TagsEditor = ({ tags, onChange, disabled = false, error = null }: Props) => {
  // Mirrors the typed text purely to drive suggestions, so the options are built before the hook
  // call that needs them.
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
  // At the cap a pick would be dropped by the hook's slice, so disable rather than swallow it.
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
    // Each tag falls into at most one bucket so messages stay focused per failure mode.
    describeInvalid: (invalid) =>
      [
        buildTooLongMessage(invalid.filter(isTooLong)),
        buildInvalidCharsMessage(invalid.filter((tag) => !isTooLong(tag) && hasInvalidChars(tag))),
      ]
        .filter(Boolean)
        .join(' ') || null,
    describeDuplicate: (tag) => `Tag "${tag}" has already been added.`,
    // An existing tag is already offered as a suggestion, so a Create row would duplicate it.
    suppressCreate: (_raw, candidate) => suggestions.some((option) => option.value === candidate),
  });

  const allMessages = [...messages, isAtMax ? `Maximum of ${MAX_TAGS} tags reached.` : null].filter(Boolean);
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
        // Not in `Select`'s Props type, so cast to satisfy tsc. react-select rejects a duplicate
        // with no chip and no callback, so Enter / Tab has to flag it explicitly.
        {...({
          onKeyDown: (e: React.KeyboardEvent) => {
            if (e.key === 'Enter' || e.key === 'Tab') {
              flagDuplicateAttempt();
            }
          },
        } as object)}
        inputId="event-definition-tags"
        aria-label="Event Definition Tags"
        multi
        allowCreate
        options={suggestions}
        isLoading={isFetching}
        disabled={disabled}
        placeholder="e.g. authentication, brute-force, compliance"
      />
      <InputDescription error={combinedError} help={HELP_TEXT} />
    </FormGroup>
  );
};

export default TagsEditor;
