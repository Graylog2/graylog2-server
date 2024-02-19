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

import AppleIcon from './icons/apple.svg';
import LinuxIcon from './icons/linux.svg';
import FreeBSDIcon from './icons/freebsd.svg';
import WindowsIcon from './icons/windows.svg';

type IconName = 'apple' | 'windows' | 'linux' | 'freebsd'

const Container = styled.div`
  padding: 3px;
  width: 24px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
`;

const Icon = styled.img`
  width: 100%;
`;

const _imageSrc = (name: string) => {
  switch (name) {
    case 'apple':
      return AppleIcon;
    case 'windows':
      return WindowsIcon;
    case 'freebsd':
      return FreeBSDIcon;
    case 'linux':
      return LinuxIcon;
    default:
      return null;
  }
};

type Props = {
  name: IconName;
}

const BrandIcon = ({ name }: Props) => {
  const imageSrc = _imageSrc(name);

  if (!imageSrc) {
    return null;
  }

  return (
    <Container>
      <Icon src={imageSrc} alt={`${name} brand icon`} />
    </Container>
  );
};

export default BrandIcon;
