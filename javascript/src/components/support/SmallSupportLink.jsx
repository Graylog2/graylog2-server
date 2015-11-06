import React from 'react';

const SmallSupportLink = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
  },
  render() {
    return (
      <p className="description-tooltips description-tooltips-small">
        <span className="fa-stack">
          <i className="fa fa-circle fa-stack-2x" />
          <i className="fa fa-lightbulb-o fa-stack-1x fa-inverse" />
        </span>
        <strong>
          {this.props.children}
        </strong>
      </p>
    );
  },
});

export default SmallSupportLink;
