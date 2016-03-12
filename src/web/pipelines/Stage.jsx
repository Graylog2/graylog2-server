import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DataTable, EntityListItem, Spinner } from 'components/common';
import RulesStore from 'rules/RulesStore';
import StageForm from './StageForm';

const Stage = React.createClass({
  propTypes: {
    stage: PropTypes.object.isRequired,
    isLastStage: PropTypes.bool,
    onUpdate: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  },
  mixins: [Reflux.connect(RulesStore)],

  _ruleHeaderFormatter(header) {
    return <th>{header}</th>;
  },

  _ruleRowFormatter(rule) {
    return (
      <tr>
        <td style={{ width: 400 }}>
          <LinkContainer to={`/system/pipelines/rules/${rule.id}`}>
            <a>{rule.title}</a>
          </LinkContainer>
        </td>
        <td>{rule.description}</td>
      </tr>
    );
  },

  _formatRules(rules) {
    const headers = ['Title', 'Description'];

    return (
      <DataTable id="processing-timeline"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._ruleHeaderFormatter}
                 rows={rules}
                 dataRowFormatter={this._ruleRowFormatter}
                 noDataText="This stage has no rules yet. Click on edit to add some."
                 filterLabel=""
                 filterKeys={[]} />
    );
  },

  render() {
    const stage = this.props.stage;

    const suffix = `Contains ${(stage.rules.length === 1 ? '1 rule' : `${stage.rules.length} rules`)}`;

    const actions = [
      <Button key="delete-stage" bsStyle="primary" onClick={this.props.onDelete}
              style={{ marginRight: 5 }}>Delete</Button>,
      <StageForm key="edit-stage" stage={stage} save={this.props.onUpdate} />,
    ];

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
    // We check if we have the rules details before trying to render them
    if (this.state.rules) {
      content = this._formatRules(this.props.stage.rules.map(name => this.state.rules.filter(r => r.title === name)[0]));
    } else {
      content = <Spinner />;
    }

    return (
      <EntityListItem title={`Stage ${stage.stage}`}
                      titleSuffix={suffix}
                      actions={actions}
                      description={description}
                      contentRow={<Col md={12}>{content}</Col>} />
    );
  },
});

export default Stage;
