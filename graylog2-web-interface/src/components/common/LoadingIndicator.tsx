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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import Spinner from 'components/common/Spinner';

import Delayed from './Delayed';

const StyledAlert = styled(Alert)`
  position: fixed;
  height: 32px;
  min-width: 200px;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  padding: 5px 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 2000;
`;

type Props = {
  text: string,
};

/**
 * Component that displays a loading indicator in the page. It uses a CSS fixed position to always appear
 * on the screen.
 *
 * Use this component when you want to load something in the background, but still provide some feedback that
 * an action is happening.
 */
const LoadingIndicator = ({ text }: Props) => (
  <Delayed delay={500}>
    <StyledAlert bsStyle="info">
      <Spinner delay={0} text={text} />
    </StyledAlert>
  </Delayed>
);

LoadingIndicator.propTypes = {
  /** Text to display while the indicator is shown. */
  text: PropTypes.string,
};

LoadingIndicator.defaultProps = {
  text: 'Loading...',
};

export default LoadingIndicator;
