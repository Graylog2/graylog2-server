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
import styled from 'styled-components';
import { useQuery, keepPreviousData } from '@tanstack/react-query';

import Select from 'components/common/Select/Select';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

type UserMatch = { username: string; full_name?: string };

type Props = {
  values: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
};

// Unit-separator character; safe because usernames should not contain it.
const VALUE_DELIMITER = '\x1F';
const SUGGESTION_LIMIT = 20;

const Hint = styled.div`
  font-size: 0.85em;
  margin-top: 0.25rem;
`;

const fetchUsers = async (query: string): Promise<UserMatch[]> => {
  const response: { users?: UserMatch[] } = await fetch(
    'GET',
    qualifyUrl(`/users/paginated?query=${encodeURIComponent(query)}&per_page=${SUGGESTION_LIMIT}`),
  );

  return response?.users ?? [];
};

const UserValueInput = ({ values, onChange, disabled = false }: Props) => {
  const [query, setQuery] = useState('');

  const { data, error } = useQuery({
    queryKey: ['users', 'search', query],
    queryFn: () => fetchUsers(query),
    enabled: !disabled && query.length > 0,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: false,
  });

  const handleChange = (joined: string) => {
    const next = joined ? joined.split(VALUE_DELIMITER).filter((v) => v.length > 0) : [];
    onChange(Array.from(new Set(next)));
  };

  // Build option list. When matches are available, label them with the user's full name (falling
  // back to the username). Ensure currently-selected usernames remain in the option list so the
  // Select can render chips with their existing label even when the lookup hasn't returned them.
  const matchOptions = (data ?? []).map((u) => ({ value: u.username, label: u.full_name ?? u.username }));
  const selectedOptions = values
    .filter((v) => !matchOptions.some((o) => o.value === v))
    .map((v) => ({ value: v, label: v }));
  const options = [...selectedOptions, ...matchOptions];

  return (
    <div data-testid="user-value-input">
      <Select
        inputId="user-values"
        aria-label="User values"
        multi
        allowCreate
        delimiter={VALUE_DELIMITER}
        options={options}
        value={values.join(VALUE_DELIMITER)}
        onChange={handleChange}
        onInputChange={(value, actionMeta) => {
          if (actionMeta?.action === 'input-change') {
            setQuery(value);
          }
        }}
        disabled={disabled}
        placeholder="Search users or enter a username"
      />
      {error && (
        <Hint data-testid="user-value-input-hint">
          User lookup unavailable ({(error as Error).message}) — enter usernames directly.
        </Hint>
      )}
    </div>
  );
};

export default UserValueInput;
