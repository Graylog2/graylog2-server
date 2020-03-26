import React from 'react';
import PropTypes from 'prop-types';
import { find, isString } from 'lodash';

import deprecationNotice from 'util/deprecationNotice';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';

import compareIconNames from './icon-fallback';

library.add(fas);

const cleanIconName = (icon) => {
  const replacedIcon = find(compareIconNames, { v4: icon });

  if (replacedIcon) {
    deprecationNotice(`You have used a deprecated \`Icon\` name. \`${icon}\` should be \`${replacedIcon.v5}\``);
    return replacedIcon.v5;
  }

  return icon;
};

/**
 * Component that renders an icon or glyph.
 * Uses Font Awesome 5 : https://fontawesome.com/icons
 *
 * No need to pass `fa` or `fa-` prefixes, just the name of the icon
 * Visit [React FontAwesome Features](https://github.com/FortAwesome/react-fontawesome#features) for more information.
 */

const Icon = ({ name, ...props }) => {
  let icon = name;
  if (isString(name)) {
    icon = cleanIconName(name.replace(/^fa-/, '')); // remove "fa-" prefix if it exists
  }

  return (
    <FontAwesomeIcon {...props} icon={icon} />
  );
};

Icon.propTypes = {
  /** Name of Font Awesome 5 Icon without `fa-` prefix */
  name: PropTypes.string.isRequired,
};

export default Icon;
