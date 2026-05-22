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
const VALID_TAG_PATTERN = /^[a-z0-9_.-]+$/;

const SUGGESTION_LIMIT = 50;
const DEBOUNCE_MS = 300;

// Unit-separator character; can't appear in a tag value (validation restricts tags to
// [a-z0-9_.-]) so it's safe to use as the delimiter Select uses to (de)serialize multi values.
const VALUE_DELIMITER = '\x1F';

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

const TagsEditor = ({ tags, onChange, disabled = false, error = null }: Props) => {
  const [input, setInput] = useState('');
  // Track only duplicate-commit attempts here. Invalid-character and over-length values are
  // committed by react-select (they land in `tags`), so they're already covered by the
  // categorization below. Duplicates are silently rejected by react-select, so we need to
  // surface them explicitly.
  const [duplicateAttempt, setDuplicateAttempt] = useState<string | null>(null);
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
    // A tag was just committed — clear the typed text. react-select's internal blur/clear
    // events are ignored below, so this is now the only path that empties the input.
    setInput('');
    setDuplicateAttempt(null);
  };

  // react-select silently rejects duplicates with no chip and no callback — flag those on
  // Enter/Tab/blur so the user gets explicit feedback. (Invalid-character and over-length
  // values do commit, so the chip-categorization below already covers them.)
  const flagDuplicateAttempt = () => {
    const trimmed = input.trim();
    if (!trimmed) return;

    const normalized = normalizeTag(trimmed);
    if (tags.includes(normalized)) {
      setDuplicateAttempt(normalized);
    }
  };

  // Each committed tag falls into at most one failure bucket so messages stay focused per
  // failure mode (too long, invalid chars, duplicate).
  const tooLongTags = tags.filter(isTooLong);
  const invalidCharTags = tags.filter((tag) => !isTooLong(tag) && hasInvalidChars(tag));
  const messages = [
    buildTooLongMessage(tooLongTags),
    buildInvalidCharsMessage(invalidCharTags),
    duplicateAttempt ? `Tag "${duplicateAttempt}" has already been added.` : null,
  ].filter((m): m is string => m !== null);

  const localValidationError =
    messages.length === 0 ? null : (
      <>
        {messages.map((m) => (
          <div key={m}>{m}</div>
        ))}
      </>
    );
  const combinedError = error ?? localValidationError;

  return (
    <FormGroup controlId="event-definition-tags" validationState={combinedError ? 'error' : null}>
      <Select
        // `Select`'s Props type doesn't surface a couple of react-select props we need here
        // (`inputValue`, `onKeyDown`), but it spreads unknown props through at runtime, so
        // we cast just these to satisfy tsc without altering behavior.
        // - `inputValue` — fully control the typed text so react-select's internal blur /
        //   menu-close clears can be ignored (otherwise Tab/blur drops the user's text on a
        //   duplicate).
        // - `onKeyDown` — flag a duplicate-commit attempt on Enter / Tab so the error
        //   message surfaces even when react-select silently rejects the commit.
        {...({
          inputValue: input,
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
        delimiter={VALUE_DELIMITER}
        options={suggestions}
        value={tags.join(VALUE_DELIMITER)}
        onChange={handleChange}
        onInputChange={(value, actionMeta) => {
          if (actionMeta?.action === 'input-change') {
            setInput(value);
            // The user is editing — clear any stale duplicate warning. It re-evaluates on
            // the next commit attempt.
            setDuplicateAttempt(null);
          }
        }}
        onBlur={flagDuplicateAttempt}
        isLoading={isFetching}
        disabled={disabled}
        placeholder="e.g. authentication, brute-force, compliance"
      />
      <InputDescription error={combinedError} help={HELP_TEXT} />
    </FormGroup>
  );
};

export default TagsEditor;
