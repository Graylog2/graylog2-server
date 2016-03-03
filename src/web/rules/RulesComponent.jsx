import React, {PropTypes} from 'react';

import { Spinner } from 'components/common';
import RuleList from './RuleList';

const RulesComponent = React.createClass({
  propTypes: {
    rules: PropTypes.array,
  },

  render() {
    if (!this.props.rules) {
      return <Spinner/>;
    }

    return (
      <div>
        <RuleList rules={this.props.rules} />
      </div>
    );
  },
});

export default RulesComponent;