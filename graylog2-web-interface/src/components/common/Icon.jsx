import React from 'react';
import PropTypes from 'prop-types';
import { find, isString } from 'lodash';

import deprecationNotice from 'util/deprecationNotice';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { faApple, faGithub, faGithubAlt, faLinux, faWindows } from '@fortawesome/free-brands-svg-icons';
import compareIconNames from './icon-fallback';

library.add(fas, faApple, faGithub, faGithubAlt, faLinux, faWindows);

const cleanIconName = (icon) => {
  const v4icon = find(compareIconNames, { v4: icon });
  const iconName = (v4icon && v4icon.v5) || icon;

  if (v4icon) {
    deprecationNotice(`You have used a deprecated \`Icon\` name. \`${icon}\` should be \`${iconName}\``);
  }

  return { prefix: 'fas', iconName };
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
  name: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.shape({
      prefix: PropTypes.string,
      iconName: PropTypes.string,
    }),
    PropTypes.arrayOf(PropTypes.string),
  ]).isRequired,
  className: PropTypes.string,
};

Icon.defaultProps = {
  className: undefined,
};

export default Icon;
