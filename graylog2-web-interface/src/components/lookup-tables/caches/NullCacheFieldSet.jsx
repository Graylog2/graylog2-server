import React, { PropTypes } from 'react';

const NullCacheFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
    // eslint-disable-next-line react/no-unused-prop-types
    validationState: PropTypes.func.isRequired,
// eslint-disable-next-line react/no-unused-prop-types
    validationMessage: PropTypes.func.isRequired,
  },

  render() {
    return null;
  },
});

export default NullCacheFieldSet;
