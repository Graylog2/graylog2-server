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

const SUGGESTION_LIMIT = 50;
const DEBOUNCE_MS = 300;

type Props = {
  tags: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
  error?: React.ReactNode;
};

const HELP_TEXT = 'Press Enter or Tab to add. Tags are lowercased and deduplicated.';

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

  const handleChange = (event: React.ChangeEvent<{ value: (string | number)[] }>) => {
    const raw = event.target.value as (string | number)[];
    const normalized = raw
      .map((value) => (typeof value === 'string' ? value : String(value)))
      .map(normalizeTag)
      .filter((value) => value.length > 0 && value.length <= MAX_TAG_LENGTH);

    onChange(Array.from(new Set(normalized)).slice(0, MAX_TAGS));
  };

  return (
    <InputList
      name="tags"
      id="event-definition-tags"
      values={[...tags]}
      onChange={handleChange}
      placeholder="e.g. authentication, brute-force, compliance"
      help={HELP_TEXT}
      error={error}
      isClearable
      isDisabled={disabled}
      suggestions={disabled ? undefined : suggestions}
      onSuggestionsInputChange={setInput}
      isLoadingSuggestions={isFetching}
    />
  );
};

export default TagsEditor;
