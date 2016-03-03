import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Col } from 'react-bootstrap';

import { DataTable, EntityListItem, Spinner } from 'components/common';
import RulesStore from 'rules/RulesStore';
import StageForm from './StageForm';

const Stage = React.createClass({
  propTypes: {
    stage: PropTypes.object.isRequired,
    isLastStage: PropTypes.bool,
    onSave: PropTypes.func.isRequired,
  },
  mixins: [Reflux.connect(RulesStore)],

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

    const actions = [
      <StageForm key="edit-stage" stage={stage} save={this.props.onSave}/>,
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
      content = (
        <Col md={12}>{this._formatRules(this.props.stage.rules.map(name => this.state.rules.filter(r => r.title === name)[0]))}</Col>
      );
    } else {
      content = <Col md={12}><Spinner/></Col>;
    }

    return (
      <EntityListItem title={`Stage ${stage.stage}`}
                      titleSuffix={suffix}
                      actions={actions}
                      description={description}
                      contentRow={content}/>
    );
  },
});

export default Stage;
