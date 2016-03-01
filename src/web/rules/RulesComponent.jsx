import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { Input, Alert } from 'react-bootstrap';

import RulesActions from './RulesActions';
import RuleList from './RuleList';

const RulesComponent = React.createClass({
  propTypes: {
    rules: PropTypes.array.isRequired,
  },

  render() {
    return (
      <div>
        <RuleList rules={this.props.rules} />
      </div>
    );
  },
});

export default RulesComponent;