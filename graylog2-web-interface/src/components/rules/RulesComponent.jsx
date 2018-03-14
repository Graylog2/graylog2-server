import PropTypes from 'prop-types';
import React from 'react';

import { Spinner } from 'components/common';
import RuleList from './RuleList';

class RulesComponent extends React.Component {
  static propTypes = {
    rules: PropTypes.array,
  };

  render() {
    if (!this.props.rules) {
      return <Spinner/>;
    }

    return (
      <div>
        <RuleList rules={this.props.rules} />
      </div>
    );
  }
}

export default RulesComponent;