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
import styled from 'styled-components';

import Select from 'components/common/Select/Select';
import { Input } from 'components/bootstrap';
import type { Matcher, MatcherType } from 'components/event-definitions/event-definitions-types';

import AssetValueInput from './AssetValueInput';

const Row = styled.div`
  display: grid;
  grid-template-columns: 9rem 1fr 1fr auto;
  gap: 0.5rem;
  align-items: start;
  padding: 0.4rem 0;
`;

const ErrorText = styled.div`
  color: ${({ theme }) => theme?.colors?.variant?.danger ?? '#a00'};
  font-size: 0.85em;
  grid-column: 1 / -1;
`;

const FIELD_VALUE_DELIMITER = '\x1F';

const validate = (m: Matcher): string | null => {
  if (m.values.length === 0) return 'Matcher must have at least one value.';

  if (m.type === 'FIELD' && (!m.field_name || m.field_name.trim() === '')) {
    return 'Field name is required for FIELD matchers.';
  }

  return null;
};

type Props = {
  matcher: Matcher;
  onChange: (next: Matcher) => void;
  onRemove: () => void;
};

const MatcherEditor = ({ matcher, onChange, onRemove }: Props) => {
  const error = validate(matcher);

  const handleTypeChange = (nextType: MatcherType) => {
    const next: Matcher = { type: nextType, values: matcher.values };

    if (nextType === 'FIELD') {
      next.field_name = matcher.field_name ?? '';
    }

    onChange(next);
  };

  const handleFieldNameChange = (fieldName: string) =>
    onChange({ ...matcher, field_name: fieldName });

  const handleValuesChange = (values: string[]) =>
    onChange({ ...matcher, values });

  const renderValueInput = () => {
    if (matcher.type === 'ASSET') {
      return <AssetValueInput values={matcher.values} onChange={handleValuesChange} />;
    }

    // FIELD: free-text creatable Select
    const options = matcher.values.map((v) => ({ label: v, value: v }));

    return (
      <Select
        inputId="matcher-field-values"
        aria-label="Field values"
        multi
        allowCreate
        delimiter={FIELD_VALUE_DELIMITER}
        value={matcher.values.join(FIELD_VALUE_DELIMITER)}
        options={options}
        onChange={(next: string) =>
          handleValuesChange(next ? next.split(FIELD_VALUE_DELIMITER).filter((v) => v.length > 0) : [])
        }
        placeholder="Enter field values"
      />
    );
  };

  return (
    <Row>
      <Input
        id="matcher-type"
        type="select"
        label=""
        aria-label="Matcher type"
        value={matcher.type}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
          handleTypeChange(e.target.value as MatcherType)
        }>
        <option value="ASSET">ASSET</option>
        <option value="FIELD">FIELD</option>
      </Input>
      {matcher.type === 'FIELD' ? (
        <Input
          id="matcher-field-name"
          type="text"
          label=""
          aria-label="Field name"
          value={matcher.field_name ?? ''}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            handleFieldNameChange(e.target.value)
          }
        />
      ) : (
        <span />
      )}
      <div>{renderValueInput()}</div>
      <button type="button" aria-label="Remove matcher" onClick={onRemove}>
        ×
      </button>
      {error && <ErrorText>{error}</ErrorText>}
    </Row>
  );
};

export default MatcherEditor;
