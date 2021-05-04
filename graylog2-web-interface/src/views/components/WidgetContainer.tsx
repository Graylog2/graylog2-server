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

import { RowContentStyles } from 'components/graylog/Row';

const Container = styled.div`
  z-index: auto;
  ${RowContentStyles}
  margin-bottom: 0;
`;

type Props = {
  isFocused: boolean,
  children: React.ReactNode,
  className?: string,
  style?: React.CSSProperties,
}

const WidgetContainer = ({ children, className, isFocused, style, ...rest }: Props) => {
  let containerStyle = {
    ...style,
    transition: 'none',
  };

  if (isFocused) {
    containerStyle = {
      ...containerStyle,
      height: '100%',
      width: '100%',
      zIndex: 5,
      top: 0,
      left: 0,
    };
  }

  return (
    <Container className={className} style={containerStyle} {...rest}>
      {children}
    </Container>
  );
};

WidgetContainer.defaultProps = {
  className: undefined,
  style: {},
};

export default WidgetContainer;
