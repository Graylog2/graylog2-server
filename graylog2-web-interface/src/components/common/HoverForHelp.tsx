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

import { OverlayTrigger } from 'components/common';
import type { SizeProp } from 'components/common/Icon';
import Icon from 'components/common/Icon';

const StyledPopover = styled.span(({ theme }) => css`
  ul {
    padding-left: 0;
  }

  li {
    margin-bottom: 5px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  h4 {
    font-size: ${theme.fonts.size.large};
  }
`);

const StyledIcon = styled(Icon)<{ $type: Type, $displayLeftMargin: boolean }>(({ theme, $type, $displayLeftMargin }) => css`
  display: inline-flex;
  color: ${$type === 'error' ? theme.colors.variant.danger : 'inherit'};
  margin: 0;
  margin-left: ${$displayLeftMargin ? '0.3em' : 0};
  pointer-events: auto !important;
`);

const iconName = (type: Type) => {
  switch (type) {
    case 'error':
      return 'error';
    case 'info':
    default:
      return 'help';
  }
};

type Type = 'info' | 'error';

type Props = {
  children: React.ReactNode,
  className?: string,
  displayLeftMargin?: boolean,
  id?: string,
  placement?: 'top' | 'right' | 'bottom' | 'left',
  iconSize?: SizeProp
  pullRight?: boolean,
  title?: string,
  testId?: string,
  trigger?: React.ComponentProps<typeof OverlayTrigger>['trigger'],
  type?: 'info' | 'error',
};

const HoverForHelp = ({
  children,
  className = '',
  displayLeftMargin = false,
  title,
  id = 'help-popover',
  pullRight = true,
  placement = 'bottom',
  testId,
  type = 'info',
  iconSize,
  trigger = ['hover', 'focus'],
}: Props) => (
  <OverlayTrigger trigger={trigger}
                  placement={placement}
                  overlay={<StyledPopover id={id}>{children}</StyledPopover>}
                  title={title}
                  testId={testId}>
    <StyledIcon className={`${className} ${pullRight ? 'pull-right' : ''}`}
                name={iconName(type)}
                type="regular"
                $type={type}
                $displayLeftMargin={displayLeftMargin}
                size={iconSize} />
  </OverlayTrigger>
);

/** @component */
export default HoverForHelp;
