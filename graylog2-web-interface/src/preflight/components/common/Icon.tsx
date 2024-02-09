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
import type { FontAwesomeIconProps } from '@fortawesome/react-fontawesome';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import type { IconLookup, IconName } from '@fortawesome/fontawesome-svg-core';
import { library } from '@fortawesome/fontawesome-svg-core';
import type { IconPrefix } from '@fortawesome/free-solid-svg-icons';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { faApple, faGithub, faGithubAlt, faLinux, faWindows } from '@fortawesome/free-brands-svg-icons';
import 'material-symbols/outlined.css';

library.add(fas, far, faApple, faGithub, faGithubAlt, faLinux, faWindows);

type IconType = 'brand' | 'solid' | 'regular';

const getPrefixForType = (type: IconType): IconPrefix => {
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

interface Props extends Omit<FontAwesomeIconProps, 'icon'> {
  name: IconName;
  type?: IconType;
}

const Icon = ({ name, type, ...props }: Props) => {
  const prefix = getPrefixForType(type);
  const iconLookup: IconLookup = { prefix, iconName: name };

  return (
    <FontAwesomeIcon {...props} icon={iconLookup} />
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
