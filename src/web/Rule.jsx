import React, {PropTypes} from 'react';

const Rule = React.createClass({
  propTypes: {
    rule: PropTypes.object.isRequired,
  },

  render() {
    return <li>
      <h2>{this.props.rule.name}</h2>
    </li>;
  }
});

export default Rule;