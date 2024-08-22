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

import { Select } from 'components/common';

const Container = styled.div`
  
`;

type Props = {
  value: string | undefined,
  onChange: (newValue: string) => void,
  options: Array<{ label: string, value: string }>,
  placeholder: string,
}

const FilterValueSelect = ({ value, onChange, options, placeholder }: Props) => (
  <Container>
    <Select placeholder={placeholder}
            aria-label={placeholder}
            menuIsOpen
            clearable={false}
            onChange={onChange}
            value={value}
            options={options} />
  </Container>
);

export default FilterValueSelect;
