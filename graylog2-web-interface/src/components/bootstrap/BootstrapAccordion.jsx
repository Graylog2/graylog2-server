import React, { PropTypes } from 'react';

const BootstrapAccordion = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },
  render() {
    return (
      <div id="bundles" className="panel-group">
        {this.props.children}
      </div>
    );
  },
});

export default BootstrapAccordion;
