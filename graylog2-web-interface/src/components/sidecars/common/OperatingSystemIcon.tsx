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
import styled from 'styled-components';

import { Icon } from 'components/common';
import BrandIcon from 'components/common/BrandIcon';

const Container = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: 5px;
  margin-left: 2px;
`;

type Props = {
  operatingSystem?: string
};

const defaultIcon = {
  iconName: 'help',
  iconType: 'default',
} as const;

const matchIcon = (_os: string) => {
  if (!_os) {
    return defaultIcon;
  }

  const os = _os.trim().toLowerCase();

  if (os.includes('darwin') || os.includes('mac os')) {
    return {
      iconName: 'apple',
      iconType: 'brand',
    } as const;
  }

  if (os.includes('linux')) {
    return {
      iconName: 'linux',
      iconType: 'brand',
    } as const;
  }

  if (os.includes('win')) {
    return {
      iconName: 'windows',
      iconType: 'brand',
    } as const;
  }

  if (os.includes('freebsd')) {
    return {
      iconName: 'freebsd',
      iconType: 'brand',
    } as const;
  }

  return defaultIcon;
};

const OperatingSystemIcon = ({ operatingSystem = undefined }: Props) => {
  const { iconName, iconType } = matchIcon(operatingSystem);

  return (
    <Container>
      {iconType === 'brand'
        ? <BrandIcon name={iconName} />
        : <Icon name={iconName} />}
    </Container>
  );
};

export default OperatingSystemIcon;
