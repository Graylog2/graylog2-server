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

export function FontAwesomeIcon({ 'data-testid': dataTestid, icon }) {
  const classNames = ['svg-inline--fa'];

  if (typeof icon === 'string') {
    classNames.push(icon);
  } else {
    classNames.push(`fa-${icon.iconName}`);
  }

  return <svg className={classNames.join(' ')} data-testid={dataTestid} />;
}

FontAwesomeIcon.propTypes = {
  icon: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.shape({
      iconName: PropTypes.string,
    }),
  ]).isRequired,
  'data-testid': PropTypes.string,
};

FontAwesomeIcon.defaultProps = {
  'data-testid': undefined,
};

export default FontAwesomeIcon;
