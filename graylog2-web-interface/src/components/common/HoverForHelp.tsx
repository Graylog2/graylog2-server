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

import { Popover, OverlayTrigger } from 'components/graylog';
import Icon from 'components/common/Icon';

type Props = {
  children: JSX.Element | JSX.Element[],
  id?: string,
  title: string,
  className: string,
  pullRight: boolean,
};

const HoverForHelp = ({ children, className, title, id, pullRight }: Props) => (
  <OverlayTrigger trigger={['hover', 'focus']}
                  placement="bottom"
                  overlay={(
                    <Popover title={title} id={id}>
                      {children}
                    </Popover>
                  )}>
    <Icon className={`${className} ${pullRight ? 'pull-right' : ''}`} name="question-circle" />
  </OverlayTrigger>
);

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
  title: PropTypes.string.isRequired,
  id: PropTypes.string,
  pullRight: PropTypes.bool,
};

HoverForHelp.defaultProps = {
  id: 'help-popover',
  className: '',
  pullRight: true,
};

export default HoverForHelp;
