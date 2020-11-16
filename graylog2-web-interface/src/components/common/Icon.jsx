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
import { find } from 'lodash';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { faApple, faGithub, faGithubAlt, faLinux, faWindows } from '@fortawesome/free-brands-svg-icons';

import deprecationNotice from 'util/deprecationNotice';

import compareIconNames from './icon-fallback';

library.add(fas, far, faApple, faGithub, faGithubAlt, faLinux, faWindows);

const removeFaPrefix = (name) => name.replace(/^fa-/, ''); // remove "fa-" prefix if it exists

const updateLegacyName = (icon) => {
  const v4icon = find(compareIconNames, { v4: icon });
  const iconName = (v4icon && v4icon.v5) || icon;

  if (v4icon) {
    deprecationNotice(`You have used a deprecated \`Icon\` name. \`${icon}\` should be \`${iconName}\``);
  }

  return iconName;
};

const cleanIconName = (name, type) => {
  const iconName = removeFaPrefix(name);

  if (type !== 'brand') {
    return updateLegacyName(iconName);
  }

  return iconName;
};

const getPrefixForType = (type) => {
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

/**
 * Component that renders an icon or glyph.
 * Uses Font Awesome 5 : https://fontawesome.com/icons
 *
 * No need to pass `fa` or `fa-` prefixes, just the name of the icon
 * Visit [React FontAwesome Features](https://github.com/FortAwesome/react-fontawesome#features) for more information.
 */

const Icon = ({ name, type, ...props }) => {
  const iconName = cleanIconName(name, type);
  const prefix = getPrefixForType(type);

  return (
    <FontAwesomeIcon {...props} icon={{ prefix, iconName }} />
  );
};

Icon.propTypes = {
  /** Name of Font Awesome 5 Icon without `fa-` prefix */
  name: PropTypes.string.isRequired,
  /**
   * Name of icon type, the brand type is needed for all brand icons.
   * The type regular is needed to outlined icon.
   * Not all icons can be outlined.
   * */
  type: PropTypes.oneOf(['brand', 'solid', 'regular']),
};

Icon.defaultProps = {
  type: 'solid',
};

export default Icon;
