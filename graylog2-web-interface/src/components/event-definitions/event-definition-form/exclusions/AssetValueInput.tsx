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

type AssetMatch = { id: string; name?: string };

type Props = {
  values: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  disabled?: boolean;
};

// Unit-separator character; safe because asset ids should not contain it.
const VALUE_DELIMITER = '\x1F';
const SUGGESTION_LIMIT = 20;

const Hint = styled.div`
  font-size: 0.85em;
  margin-top: 0.25rem;
`;

const fetchAssets = async (query: string): Promise<AssetMatch[]> => {
  const response: { results?: AssetMatch[] } | AssetMatch[] = await fetch(
    'GET',
    qualifyUrl(`/assets/search?query=${encodeURIComponent(query)}&limit=${SUGGESTION_LIMIT}`),
  );

  return Array.isArray(response) ? response : (response?.results ?? []);
};

const AssetValueInput = ({ values, onChange, disabled = false }: Props) => {
  const [query, setQuery] = useState('');

  const { data, error } = useQuery({
    queryKey: ['assets', 'search', query],
    queryFn: () => fetchAssets(query),
    enabled: !disabled && query.length > 0,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: false,
  });

  const handleChange = (joined: string) => {
    const next = joined ? joined.split(VALUE_DELIMITER).filter((v) => v.length > 0) : [];
    onChange(Array.from(new Set(next)));
  };

  // Build option list. When matches are available, label them with the asset name (falling back
  // to the id). Ensure currently-selected ids remain in the option list so the Select can render
  // chips with their existing label even when the lookup hasn't returned them.
  const matchOptions = (data ?? []).map((a) => ({ value: a.id, label: a.name ?? a.id }));
  const selectedOptions = values
    .filter((v) => !matchOptions.some((o) => o.value === v))
    .map((v) => ({ value: v, label: v }));
  const options = [...selectedOptions, ...matchOptions];

  return (
    <div data-testid="asset-value-input">
      <Select
        inputId="asset-values"
        aria-label="Asset values"
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
        placeholder="Search assets or enter an id"
      />
      {error && (
        <Hint data-testid="asset-value-input-hint">
          Asset lookup unavailable ({(error as Error).message}) — enter ids directly.
        </Hint>
      )}
    </div>
  );
};

export default AssetValueInput;
