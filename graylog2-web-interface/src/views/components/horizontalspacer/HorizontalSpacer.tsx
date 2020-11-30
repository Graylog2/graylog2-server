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
import PropTypes from 'prop-types';
import styled from 'styled-components';

/**
 * Component to display horizontal space.
 * Mainly needed for responsive bootstrap grids.
 * Can be used in combination with bootstrap Cols to show horizontal space only for specific viewports.
 */

type Props = {
  /** Needed height in px */
  height: number,
  /** Allow custom classNames */
  className?: string,
};

const Spacer = styled.div<{ height: number }>`
  width: 100%;
  height: ${(props) => props.height}px;
`;

const HorizontalSpacer = ({ height, className }: Props) => <Spacer height={height} className={className} />;

HorizontalSpacer.propTypes = {
  height: PropTypes.number,
  className: PropTypes.string,
};

HorizontalSpacer.defaultProps = {
  height: 10,
  className: undefined,
};

export default HorizontalSpacer;
