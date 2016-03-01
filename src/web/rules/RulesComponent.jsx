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
    let rules;
    if (this.props.rules.length == 0) {
      rules =
        <Alert bsStyle='warning'>
          <i className="fa fa-info-circle" />&nbsp; No rules configured.
      </Alert>
    } else {
      rules = <RuleList rules={this.props.rules} />;
    }

    return (
      <div>
        {rules}
      </div>
    );
  },
});

export default RulesComponent;