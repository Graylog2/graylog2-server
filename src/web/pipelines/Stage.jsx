import React, {PropTypes} from 'react';
import { Col } from 'react-bootstrap';

import { DataTable, EntityListItem, Spinner } from 'components/common';
import RulesActions from 'rules/RulesActions';

const Stage = React.createClass({
  propTypes: {
    stage: PropTypes.object.isRequired,
    isLastStage: PropTypes.bool,
  },

  getInitialState() {
    return {
      rules: undefined,
    };
  },


  componentDidMount() {
    RulesActions.multiple(this.props.stage.rules, (rules) => {
      const newRules = this.props.stage.rules.map(ruleName => rules.filter(rule => rule.title === ruleName)[0]);
      this.setState({rules: newRules});
    });
  },

  _ruleHeaderFormatter(header) {
    return <th>{header}</th>;
  },

  _ruleRowFormatter(rule) {
    return (
      <tr>
        <td>{rule.title}</td>
        <td>{rule.description}</td>
        <td></td>
      </tr>
    );
  },

  _formatRules(rules) {
    const headers = ['Title', 'Description', 'Actions'];

    return (
      <DataTable id="processing-timeline"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._ruleHeaderFormatter}
                 rows={rules}
                 dataRowFormatter={this._ruleRowFormatter}
                 filterLabel=""
                 filterKeys={[]}/>
    );
  },

  render() {
    const stage = this.props.stage;

    const suffix = `Contains ${(stage.rules.length === 1 ? '1 rule' : `${stage.rules.length} rules` )}`;

    let description;
    if (this.props.isLastStage) {
      description = 'There are no further stages in this pipeline. Once rules in this stage are applied, the pipeline will have finished processing.';
    } else {
      description = (
        <span>
          Messages satisfying <strong>{stage.match_all ? 'all rules' : 'at least one rule'}</strong>{' '}
          in this stage, will continue to the next stage.
        </span>
      );
    }

    let content;
    if (this.state.rules) {
      content = (
        <Col md={12}>{this._formatRules(this.state.rules)}</Col>
      );
    } else {
      content = <Col md={12}><Spinner/></Col>;
    }

    return (
      <EntityListItem title={`Stage ${stage.stage}`}
                      titleSuffix={suffix}
                      description={description}
                      contentRow={content}/>
    );
  },
});

export default Stage;
