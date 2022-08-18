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
import { useRef } from 'react';
import styled from 'styled-components';

import useElementDimensions from 'hooks/useElementDimensions';

const Wrapper = styled.div`
  height: 100%;
  width: 100%;
  overflow: hidden;
  grid-row: 2;
  grid-column: 1;
  -ms-grid-row: 2;
  -ms-grid-column: 1;
`;

type Dimensions = { height: number; width: number; };

type Props = {
  children: (dimensions: Dimensions) => React.ReactElement,
};

const FullSizeContainer = ({ children }: Props) => {
  const element = useRef<HTMLDivElement>(null);
  const { width, height } = useElementDimensions(element);

  return (
    <Wrapper ref={element}>
      {children({ height, width })}
    </Wrapper>
  );
};

export default FullSizeContainer;
