import PropTypes from 'prop-types';
import React from 'react';

class BootstrapAccordion extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

  render() {
    return (
      <div id="bundles" className="panel-group">
        {this.props.children}
      </div>
    );
  }
}

export default BootstrapAccordion;
