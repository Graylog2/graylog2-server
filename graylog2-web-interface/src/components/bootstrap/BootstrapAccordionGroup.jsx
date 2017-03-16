import React, { PropTypes } from 'react';

const BootstrapAccordionGroup = React.createClass({
  propTypes: {
    name: PropTypes.string,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },
  render() {
    let name;
    let id;

    if (this.props.name) {
      name = this.props.name;
      id = name.replace(/[^0-9a-zA-Z]/g, '-').toLowerCase();
    }

    return (
      <div className="panel panel-default">
        <div className="panel-heading">
          <h4 className="panel-title">
            <a href={`#${id}`} data-parent="#bundles" data-toggle="collapse" className="collapsed">{name}</a>
          </h4>
        </div>
        <div className="panel-collapse collapse" id={id}>
          <div className="panel-body">
            {this.props.children}
          </div>
        </div>
      </div>
    );
  },
});

export default BootstrapAccordionGroup;
