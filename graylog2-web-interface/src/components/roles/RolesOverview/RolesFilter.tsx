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
import { $PropertyType } from 'utility-types';

import { SearchForm } from 'components/common';
import type { Pagination } from 'stores/PaginationTypes';

import RolesQueryHelper from '../RolesQueryHelper';

type Props = {
  onSearch: (query: $PropertyType<Pagination, 'query'>) => void,
};

const Container = styled.div`
  margin-bottom: 10px;
`;

const RolesFilter = ({ onSearch }: Props) => (
  <Container>
    <SearchForm onSearch={onSearch}
                onReset={() => onSearch('')}
                queryHelpComponent={<RolesQueryHelper />}
                topMargin={0} />
  </Container>
);

export default RolesFilter;
