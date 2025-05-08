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
import DOMPurify from 'dompurify';
import styled from 'styled-components';
import { useMemo } from 'react';

import useNavigationCustomization from 'brand-customization/useNavigationCustomization';
import { Icon } from 'components/common';
import type { IconName } from 'components/common/Icon';
import type { Branding } from 'util/AppConfig';

type NavIconType = keyof Branding['navigation'];

const DEFAULT_ICONS: Record<NavIconType, IconName> = {
  'home': 'home',
  'scratchpad': 'edit_square',
  'user_menu': 'person',
  'help': 'help',
};

const SvgContainer = styled.div`
  svg {
    width: 100%;
    height: 22px;
    display: block;
    fill: currentColor;
  }
`;

const useCustomIcon = (type: NavIconType) => {
  const navigationCustomization = useNavigationCustomization();
  const customIcon = navigationCustomization?.[type]?.icon;

  return useMemo(() => {
    if (customIcon) {
      return DOMPurify.sanitize(customIcon);
    }

    return null;
  }, [customIcon]);
};

type Props = {
  type: NavIconType;
  title?: string;
};

const NavIcon = ({ type, title = undefined }: Props) => {
  const customSvgIcon = useCustomIcon(type);

  if (customSvgIcon) {
    return <SvgContainer dangerouslySetInnerHTML={{ __html: customSvgIcon }} title={title} />;
  }

  return <Icon name={DEFAULT_ICONS[type]} size="lg" title={title} />;
};

export default NavIcon;
