import React from 'react';
import PropTypes from 'prop-types';
import loadable from 'loadable-components';

const ErrorComponent = ({ error }) => <div>Loading component failed: {error.message}</div>;
ErrorComponent.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string,
  }).isRequired,
};

export default (f) => loadable(() => f().then((c) => c.default), { ErrorComponent });
