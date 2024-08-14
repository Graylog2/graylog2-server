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

import FilterRuleEditButton from 'components/streams/StreamDetails/output-filter/FilterRuleEditButton';
import FilterDeleteButton from 'components/streams/StreamDetails/output-filter/FilterDeleteButton';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';

type Props = {
  filterRule: StreamOutputFilterRule,
  destinationType: string,
}
const ActionWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const FilterActions = ({ filterRule, destinationType }: Props) => (
  <ActionWrapper>
    <FilterRuleEditButton filterRule={filterRule}
                          destinationType={destinationType}
                          streamId={filterRule.stream_id} />
    <FilterDeleteButton streamId={filterRule.stream_id} filterOutputRule={filterRule} />
  </ActionWrapper>
);

export default FilterActions;
