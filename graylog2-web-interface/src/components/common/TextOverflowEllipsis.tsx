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

const Wrapper = styled.div`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  className?: string,
  children: string,
  titleOverride?: string,
};

/**
 * Component that signals text overflow to users by using an ellipsis.
 * The parent component needs a concrete width.
 */
const TextOverflowEllipsis = ({ children, titleOverride, className }: Props) => (
  <Wrapper title={titleOverride || children} className={className}>
    {children}
  </Wrapper>
);

TextOverflowEllipsis.defaultProps = {
  className: undefined,
  titleOverride: undefined,
};

export default TextOverflowEllipsis;
