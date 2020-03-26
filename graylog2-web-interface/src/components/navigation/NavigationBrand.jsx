import React from 'react';
import PropTypes from 'prop-types';

import logoUrl from 'images/toplogo.svg';

// Don't pass active prop, since `a` tag doesn't support it.
// eslint-disable-next-line no-unused-vars
function BrandComponent({ active, ...props }) {
  return <a {...props}><img src={logoUrl} alt="Graylog logo" width="90" /></a>;
}

BrandComponent.propTypes = {
  active: PropTypes.bool,
};

BrandComponent.defaultProps = {
  active: false,
};

export default BrandComponent;
