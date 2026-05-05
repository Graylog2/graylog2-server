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

import { InputList } from 'components/common';

// Mirror of EventDefinitionDto.normalizeTags / TagNormalizer on the server.
// Keep these two definitions in sync.
const normalizeTag = (raw: string): string => raw.trim().toLowerCase();

type Props = {
  tags: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
  error?: React.ReactNode;
};

const HELP_TEXT = 'Press Enter or Tab to add. Tags are lowercased and deduplicated.';

const TagsEditor = ({ tags, onChange, disabled = false, error = null }: Props) => {
  const handleChange = (event: React.ChangeEvent<{ value: (string | number)[] }>) => {
    const raw = event.target.value as (string | number)[];
    const normalized = raw
      .map((value) => (typeof value === 'string' ? value : String(value)))
      .map(normalizeTag)
      .filter((value) => value.length > 0);

    onChange(Array.from(new Set(normalized)));
  };

  return (
    <InputList
      name="tags"
      id="event-definition-tags"
      label="Tags"
      values={[...tags]}
      onChange={handleChange}
      placeholder="e.g. authentication, brute-force, compliance"
      help={HELP_TEXT}
      error={error}
      isClearable
      isDisabled={disabled}
    />
  );
};

export default TagsEditor;
