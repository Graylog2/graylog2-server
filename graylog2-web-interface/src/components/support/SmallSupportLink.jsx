import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Icon } from 'components/common';

const IconStack = styled.span`
  margin-right: 1px;
  position: relative;
  top: -1px;
`;

class SmallSupportLink extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
  };

  render() {
    return (
      <p className="description-tooltips">
        <IconStack className="fa-stack">
          <Icon name="circle" className="fa-stack-2x" />
          <Icon name="lightbulb-o" className="fa-stack-1x" inverse />
        </IconStack>
        <strong>
          {this.props.children}
        </strong>
      </p>
    );
  }
}

export default SmallSupportLink;
