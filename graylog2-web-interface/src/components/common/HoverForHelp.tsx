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
import styled, { css } from 'styled-components';
import type { SizeProp } from '@fortawesome/fontawesome-svg-core';

import { OverlayTrigger } from 'components/common';
import { Popover } from 'components/bootstrap';
import Icon from 'components/common/Icon';

const StyledPopover = styled(Popover)(({ theme }) => css`
  ul {
    padding-left: 0;
  }

  li {
    margin-bottom: 5px;

    :last-child {
      margin-bottom: 0;
    }
  }

  h4 {
    font-size: ${theme.fonts.size.large};
  }
`);

const StyledIcon = styled(Icon)<{ $type: Type }>(({ theme, $type }) => css`
  color: ${$type === 'error' ? theme.colors.variant.danger : 'inherit'};
`);

const iconName = (type: Type) => {
  switch (type) {
    case 'error':
      return 'circle-exclamation';
    case 'info':
    default:
      return 'question-circle';
  }
};

type Type = 'info' | 'error';

type Props = {
  children: React.ReactNode,
  className?: string,
  id?: string,
  placement?: 'top' | 'right' | 'bottom' | 'left',
  iconSize?: SizeProp
  pullRight?: boolean,
  title?: string,
  testId?: string,
  trigger?: React.ComponentProps<typeof OverlayTrigger>['trigger'],
  type?: 'info' | 'error',
};

const HoverForHelp = ({ children, className, title, id, pullRight, placement, testId, type, iconSize, trigger }: Props) => (
  <OverlayTrigger trigger={trigger}
                  placement={placement}
                  overlay={<StyledPopover title={title} id={id}>{children}</StyledPopover>}
                  testId={testId}>
    <StyledIcon className={`${className} ${pullRight ? 'pull-right' : ''}`} name={iconName(type)} $type={type} size={iconSize} />
  </OverlayTrigger>
);

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
  id: PropTypes.string,
  placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
  pullRight: PropTypes.bool,
  title: PropTypes.string,
  testId: PropTypes.string,
  trigger: PropTypes.arrayOf(PropTypes.oneOf(['click', 'focus', 'hover'])) || PropTypes.oneOf(['click', 'focus', 'hover']),
};

HoverForHelp.defaultProps = {
  id: 'help-popover',
  className: '',
  pullRight: true,
  placement: 'bottom',
  testId: undefined,
  title: undefined,
  type: 'info',
  iconSize: undefined,
  trigger: ['hover', 'focus'],
};

/** @component */
export default HoverForHelp;
