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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const arrowSize = 10;
const StyledTooltip = styled(BootstrapTooltip)(({ theme }) => css`
  &.in {
    opacity: 1;
    filter: drop-shadow(0 0 3px ${theme.colors.variant.lighter.default});
  }

  &.top {
    .tooltip-arrow {
      border-top-color: ${theme.colors.global.contentBackground};
      border-width: ${arrowSize}px ${arrowSize}px 0;
      margin-left: -${arrowSize}px;
      bottom: -${arrowSize / 2}px;
    }
  }
  
  &.right {
    .tooltip-arrow {
      border-right-color: ${theme.colors.global.contentBackground};
      border-width: ${arrowSize}px ${arrowSize}px ${arrowSize}px 0;
      margin-top: -${arrowSize}px;
      left: -${arrowSize / 2}px;
    }
  }

  &.bottom {
    .tooltip-arrow {
      border-bottom-color: ${theme.colors.global.contentBackground};
      border-width: 0 ${arrowSize}px ${arrowSize}px;
      margin-left: -${arrowSize}px;
      top: -${arrowSize / 2}px;
    }
  }
  
  &.left {
    .tooltip-arrow {
      border-left-color: ${theme.colors.global.contentBackground};
      border-width: ${arrowSize}px 0 ${arrowSize}px ${arrowSize}px;
      margin-top: -${arrowSize}px;
      right: -${arrowSize / 2}px;
    }
  }
  
  .tooltip-inner {
    color: ${theme.colors.global.textDefault};
    background-color: ${theme.colors.global.contentBackground};
    max-width: 300px;
    opacity: 1;

    .datapoint-info {
      text-align: left;

      .date {
        color: ${theme.colors.variant.darkest.default};
      }
    }
  }
`);

const Tooltip = ({ children, className, id, placement, positionTop, positionLeft, arrowOffsetTop, arrowOffsetLeft }) => {
  return (
    <StyledTooltip className={className}
                   id={id}
                   placement={placement}
                   positionTop={positionTop}
                   positionLeft={positionLeft}
                   arrowOffsetTop={arrowOffsetTop}
                   arrowOffsetLeft={arrowOffsetLeft}>
      {children}
    </StyledTooltip>
  );
};

Tooltip.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  /**
   * An html id attribute, necessary for accessibility
   * @type {string|number}
   * @required
   */
  id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,

  /**
   * Sets the direction the Tooltip is positioned towards.
   */
  placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),

  /**
   * The "top" position value for the Tooltip.
   */
  positionTop: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  /**
   * The "left" position value for the Tooltip.
   */
  positionLeft: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),

  /**
   * The "top" position value for the Tooltip arrow.
   */
  arrowOffsetTop: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  /**
   * The "left" position value for the Tooltip arrow.
   */
  arrowOffsetLeft: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
};

Tooltip.defaultProps = {
  className: undefined,
  placement: 'right',
  positionTop: undefined,
  positionLeft: undefined,
  arrowOffsetTop: undefined,
  arrowOffsetLeft: undefined,
};

/** @component */
export default Tooltip;
