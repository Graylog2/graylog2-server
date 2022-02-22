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
import { find } from 'lodash';
import type { IconName } from '@fortawesome/fontawesome-common-types';
import type { SizeProp } from '@fortawesome/fontawesome-svg-core';

import deprecationNotice from 'util/deprecationNotice';
import loadAsync from 'routing/loadAsync';

import compareIconNames from './icon-fallback';

const CustomFontAwesomeIcon = loadAsync(() => import('./CustomFontAwesomeIcon'));

type IconTypes = 'brand' | 'regular' | 'solid';

const removeFaPrefix = (name: string) => name.replace(/^fa-/, ''); // remove "fa-" prefix if it exists

const updateLegacyName = (icon: string) => {
  const v4icon = find(compareIconNames, { v4: icon });
  const iconName = (v4icon && v4icon.v5) || icon;

  if (v4icon) {
    deprecationNotice(`You have used a deprecated \`Icon\` name. \`${icon}\` should be \`${iconName}\``);
  }

  return iconName;
};

const cleanIconName = (name: string, type: IconTypes) => {
  const iconName = removeFaPrefix(name);

  if (type !== 'brand') {
    return updateLegacyName(iconName);
  }

  return iconName;
};

const getPrefixForType = (type: IconTypes) => {
  switch (type) {
    case 'brand':
      return 'fab';
    case 'regular':
      return 'far';
    case 'solid':
    default:
      return 'fas';
  }
};

type Props = {
  className?: string,
  'data-testid'?: string,
  /** Name of Font Awesome 5 Icon without `fa-` prefix */
  name: IconName,
  size?: SizeProp,
  spin?: boolean,
  /**
   * Name of icon type, the brand type is needed for all brand icons.
   * The type regular is needed to outlined icon.
   * Not all icons can be outlined.
   * */
  type?: IconTypes,
  fixedWidth?: boolean,
  inverse?: boolean,
  style?: React.CSSProperties,
  onClick?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onMouseEnter?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onMouseLeave?: (event: React.MouseEvent<SVGSVGElement>) => void,
  onFocus?: (event: React.FocusEvent<SVGSVGElement>) => void,
  tabIndex?: number,
  title?: string,
}

/**
 * Component that renders an icon or glyph.
 * Uses Font Awesome 5 : https://fontawesome.com/icons
 *
 * No need to pass `fa` or `fa-` prefixes, just the name of the icon
 * Visit [React FontAwesome Features](https://github.com/FortAwesome/react-fontawesome#features) for more information.
 */

const Icon = ({
  name,
  type,
  size,
  className,
  spin,
  fixedWidth,
  inverse,
  style,
  'data-testid': testId,
  onClick,
  onMouseEnter,
  onMouseLeave,
  onFocus,
  tabIndex,
  title,
}: Props) => {
  const iconName = cleanIconName(name, type);
  const prefix = getPrefixForType(type);

  return (
    <CustomFontAwesomeIcon className={className}
                           data-testid={testId}
                           fixedWidth={fixedWidth}
                           icon={{ prefix, iconName }}
                           inverse={inverse}
                           size={size}
                           spin={spin}
                           style={style}
                           onClick={onClick}
                           onMouseEnter={onMouseEnter}
                           onMouseLeave={onMouseLeave}
                           tabIndex={tabIndex}
                           title={title}
                           onFocus={onFocus} />
  );
};

Icon.defaultProps = {
  className: undefined,
  'data-testid': undefined,
  fixedWidth: false,
  inverse: false,
  size: undefined,
  spin: false,
  style: undefined,
  type: 'solid',
  onClick: undefined,
  onMouseEnter: undefined,
  onMouseLeave: undefined,
  onFocus: undefined,
  tabIndex: undefined,
  title: undefined,
};

export type { IconName };
export default Icon;
