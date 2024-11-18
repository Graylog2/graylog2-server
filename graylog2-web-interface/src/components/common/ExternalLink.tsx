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
import trim from 'lodash/trim';

import type { IconName } from 'components/common/Icon';

import Icon from './Icon';

type Props = {
  href?: string,
  children: React.ReactNode,
  target?: string,
  iconName?: IconName,
  className?: string,
};

/**
 * Component that renders a link to an external resource.
 */
const ExternalLink = ({ children, className = '', href = '', iconName = 'open_in_new', target = '_blank' }: Props) => {
  const content = (
    <span>
      {children}
      &nbsp;
      <Icon name={iconName} />
    </span>
  );

  // This makes the component usable as child element of a component that already renders a link (e.g. MenuItem)
  if (trim(href) === '') {
    return content;
  }

  return (
    <a href={href} target={target} className={className} rel="noopener noreferrer">
      {content}
    </a>
  );
};

export default ExternalLink;
