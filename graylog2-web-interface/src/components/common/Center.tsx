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

const Container = styled.div<{ $fullHeight: boolean }>(({ $fullHeight }) => `
  display: flex;
  height: ${$fullHeight ? '100%' : 'auto'};
  justify-content: center;
  align-items: center;
`);

type Props = {
  children: React.ReactNode,
  fullHeight?: boolean,
};

/**
 * This component centers its children horizontally and vertically.
 */
const Center = ({ children, fullHeight }: Props) => (
  <Container $fullHeight={fullHeight}>
    {children}
  </Container>
);

Center.defaultProps = {
  fullHeight: true,
};

export default Center;
