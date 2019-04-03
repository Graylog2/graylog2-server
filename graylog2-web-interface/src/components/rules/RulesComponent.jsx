import PropTypes from 'prop-types';
import React from 'react';

import { Spinner } from 'components/common';
import RuleList from './RuleList';

class RulesComponent extends React.Component {
  static propTypes = {
    rules: PropTypes.array,
    onDelete: PropTypes.func,
  };

  static defaultProps = {
    rules: [],
    onDelete: () => {},
  };

  render() {
    if (!this.props.rules) {
      return <Spinner />;
    }

    return (
      <div>
        <RuleList rules={this.props.rules} onDelete={this.props.onDelete} />
      </div>
    );
  }
}

export default RulesComponent;
