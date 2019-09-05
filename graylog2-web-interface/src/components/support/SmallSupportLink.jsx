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
          <Icon name="circle" className="fa-stack-2x" />
          <Icon name="lightbulb-o" className="fa-stack-1x" inverse />
        </span>
        <strong>
          {this.props.children}
        </strong>
      </p>
    );
  }
}

export default SmallSupportLink;
