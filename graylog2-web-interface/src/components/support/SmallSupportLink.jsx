import PropTypes from 'prop-types';
import React from 'react';
import { Icon } from 'components/graylog';

class SmallSupportLink extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
  };

  render() {
    return (
      <p className="description-tooltips description-tooltips-small">
        <span className="fa-stack">
          <Icon className="fa fa-circle fa-stack-2x" />
          <Icon className="fa fa-lightbulb-o fa-stack-1x fa-inverse" />
        </span>
        <strong>
          {this.props.children}
        </strong>
      </p>
    );
  }
}

export default SmallSupportLink;
