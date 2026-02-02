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

import NoAttributeProvided from 'components/event-definitions/replay-search/NoAttributeProvided';

export const Item = styled.div`
  display: flex;
  gap: 5px;
  align-items: flex-start;
`;
export const Value = styled.div`
  display: flex;
  word-break: break-all;
`;

const Title = styled.b`
  min-width: 85px;
`;
const EventAttribute = ({ children = null, title }: React.PropsWithChildren<{ title: string }>) => (
  <Item key={title}>
    <Title>{title}: </Title>
    <Value title={title}>{children || <NoAttributeProvided name={title} />}</Value>
  </Item>
);

export default EventAttribute;
