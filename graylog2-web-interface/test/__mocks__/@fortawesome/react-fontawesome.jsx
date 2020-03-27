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
