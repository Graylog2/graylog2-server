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

import { Center, Icon } from 'components/common';

const Description = styled.div`
  max-width: 700px;
  display: flex;
  flex-direction: column;
`;

const ErrorIcon = styled(Icon)`
  margin-left: 15px;
  margin-right: 15px;
`;

type Props = {
  error: React.ReactNode,
  title: React.ReactNode,
}
const Error = ({ error, title }: Props) => (
  <Center>
    <ErrorIcon name="warning" size="3x" />
    <Description>
      <strong>{title}</strong>
      <span>{error}</span>
    </Description>
  </Center>
);

export default Error;
