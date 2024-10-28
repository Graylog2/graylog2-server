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
import styled, { css } from 'styled-components';
import { useEffect, useState } from 'react';

import { Alert } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';

import Delayed from './Delayed';

const Container = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  position: fixed;
  min-width: 200px;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 2px 10px rgb(0 0 0 / 20%);
  z-index: 2000;
`);

const StyledAlert = styled(Alert)`
  margin: 0;
  height: 32px;
  padding: 5px 20px;
  text-align: center;
`;

type Props = {
  text?: string
  longWaitText?: string
  longWaitTimeout?: number
};

/**
 * Component that displays a loading indicator in the page. It uses a CSS fixed position to always appear
 * on the screen.
 *
 * Use this component when you want to load something in the background, but still provide some feedback that
 * an action is happening.
 */
const LoadingIndicator = ({ text = 'Loading...', longWaitText = 'This is taking a bit longer, please hold on...', longWaitTimeout = 20000 }: Props) => {
  const [indicatorText, setIndicatorText] = useState(text);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setIndicatorText(longWaitText);
    }, longWaitTimeout);

    return () => clearTimeout(timeoutId);
  }, [longWaitText, longWaitTimeout]);

  return (
    <Delayed delay={500}>
      <Container>
        <StyledAlert bsStyle="info">
          <Spinner delay={0} text={indicatorText} />
        </StyledAlert>
      </Container>
    </Delayed>
  );
};

export default LoadingIndicator;
