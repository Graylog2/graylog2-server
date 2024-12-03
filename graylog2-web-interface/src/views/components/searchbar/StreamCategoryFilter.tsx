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
import React from 'react';
import styled from 'styled-components';

import Select from 'components/common/Select';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div`
  flex: 1;
  grid-area: streams;
`;

type Props = {
  disabled?: boolean
  value?: Array<string>
  streamCategories: Array<{ key: string, value: string }>,
  onChange: (newStreamCategories: Array<string>) => void,
};

const StreamCategoryFilter = ({ disabled = false, value = [], streamCategories, onChange }: Props) => {
  const selectedCategories = value.join(',');
  const placeholder = 'Select stream categories the search should include.';
  const options = streamCategories.sort(({ key: key1 }, { key: key2 }) => defaultCompare(key1, key2));

  const handleChange = (selected: string) => {
    onChange(selected === '' ? [] : selected.split(','));
  };

  if (!streamCategories || streamCategories.length < 1) return null;

  return (
    <Container data-testid="stream-category-filter" title={placeholder}>
      <Select placeholder={placeholder}
              disabled={disabled}
              displayKey="key"
              inputId="stream-categories-filter"
              onChange={handleChange}
              options={options}
              multi
              value={selectedCategories} />
    </Container>
  );
};

export default StreamCategoryFilter;
