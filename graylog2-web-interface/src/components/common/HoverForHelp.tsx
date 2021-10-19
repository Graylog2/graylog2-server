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

type Props = {
  children: React.ReactNode,
  className?: string,
  id?: string,
  placement?: 'top' | 'right' | 'bottom' | 'left',
  pullRight?: boolean,
  title: string,
  testId?: string,
};

const HoverForHelp = ({ children, className, title, id, pullRight, placement, testId }: Props) => (
  <OverlayTrigger trigger={['hover', 'focus']}
                  placement={placement}
                  overlay={(
                    <StyledPopover title={title} id={id}>
                      {children}
                    </StyledPopover>
                  )}
                  testId={testId}>
    <Icon className={`${className} ${pullRight ? 'pull-right' : ''}`} name="question-circle" />
  </OverlayTrigger>
);

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
  id: PropTypes.string,
  placement: PropTypes.oneOf(['top', 'right', 'bottom', 'left']),
  pullRight: PropTypes.bool,
  title: PropTypes.string.isRequired,
  testId: PropTypes.string,
};

HoverForHelp.defaultProps = {
  id: 'help-popover',
  className: '',
  pullRight: true,
  placement: 'bottom',
  testId: undefined,
};

/** @component */
export default HoverForHelp;
