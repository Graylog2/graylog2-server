import React, {PropTypes} from 'react';
import DataTable from 'components/common/DataTable';
import Timestamp from 'components/common/Timestamp';

import { Button } from 'react-bootstrap';

import RuleForm from './RuleForm';
import RulesActions from './RulesActions';

const RuleList = React.createClass({
  propTypes: {
    rules: PropTypes.array.isRequired
  },

  _save(rule, callback) {
    if (rule.id) {
      RulesActions.update(rule);
    } else {
      RulesActions.save(rule);
    }
    callback();
  },

  _delete(rule) {
    if (window.confirm('Do you really want to delete rule ' + rule.title + '?')) {
      RulesActions.delete(rule.id);
    }
  },

  _validateRule(rule, setErrorsCb) {
    RulesActions.parse(rule, setErrorsCb);
  },

  _headerCellFormatter(header) {
    return <th>{header}</th>;
  },
  _ruleInfoFormatter(rule) {
    let actions = [
      <button key="delete" className="btn btn-primary btn-xs" onClick={() => this._delete(rule)} title="Delete rule">Delete</button>,
      <span key="space">&nbsp;</span>,
      <RuleForm key="edit" rule={rule} validateRule={this._validateRule} save={this._save} />,
    ];

    return (
      <tr key={rule.title}>
        <td>{rule.title}</td>
        <td className="limited">{rule.description}</td>
        <td className="limited"><Timestamp dateTime={rule.created_at} relative={true}/></td>
        <td className="limited"><Timestamp dateTime={rule.modified_at} relative={true}/></td>
        <td style={{width: 150}}>{actions}</td>
      </tr>
    );
  },
  render() {
    var filterKeys = ["title", "description"];
    var headers = ["Title", "Description", "Created at", "Last modified", "Actions"];

    return (
      <div>
        <DataTable id="rule-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey={"title"}
                   rows={this.props.rules}
                   filterBy="Title"
                   dataRowFormatter={this._ruleInfoFormatter}
                   filterLabel="Filter Rules"
                   filterKeys={filterKeys}>
          <div className="pull-right">
            <RuleForm create
                      save={this._save}
                      validateRule={this._validateRule}
            />
          </div>
        </DataTable>
      </div>
    );
  }
});

export default RuleList;
