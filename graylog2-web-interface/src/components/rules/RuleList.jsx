import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';

import { Button, ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { DataTable, Timestamp } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import RuleMetricsConfigContainer from './RuleMetricsConfigContainer';

const { RulesActions } = CombinedProvider.get('Rules');

class RuleList extends React.Component {
  static propTypes = {
    rules: PropTypes.array.isRequired,
  };

  state = {
    openMetricsConfig: false,
  };

  _delete = (rule) => {
    return () => {
      if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
        RulesActions.delete(rule);
      }
    };
  };

  _headerCellFormatter = (header) => {
    return <th>{header}</th>;
  };

  _ruleInfoFormatter = (rule) => {
    const actions = [
      <Button key="delete" bsStyle="primary" bsSize="xsmall" onClick={this._delete(rule)} title="Delete rule">
        Delete
      </Button>,
      <span key="space">&nbsp;</span>,
      <LinkContainer key="edit" to={Routes.SYSTEM.PIPELINES.RULE(rule.id)}>
        <Button bsStyle="info" bsSize="xsmall">Edit</Button>
      </LinkContainer>,
    ];

    return (
      <tr key={rule.title}>
        <td>
          <Link to={Routes.SYSTEM.PIPELINES.RULE(rule.id)}>
            {rule.title}
          </Link>
        </td>
        <td className="limited">{rule.description}</td>
        <td className="limited"><Timestamp dateTime={rule.created_at} relative /></td>
        <td className="limited"><Timestamp dateTime={rule.modified_at} relative /></td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.executed`} zeroOnMissing>
            <CounterRate suffix="msg/s" />
          </MetricContainer>
        </td>
        <td>
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Rule.${rule.id}.failed`}>
            <CounterRate showTotal suffix="errors/s" hideOnMissing />
          </MetricContainer>
        </td>
        <td className="actions">{actions}</td>
      </tr>
    );
  };

  toggleMetricsConfig = () => {
    const { openMetricsConfig } = this.state;
    this.setState({ openMetricsConfig: !openMetricsConfig });
  };

  render() {
    const filterKeys = ['title', 'description'];
    const headers = ['Title', 'Description', 'Created', 'Last modified', 'Throughput', 'Errors', 'Actions'];
    const { openMetricsConfig } = this.state;

    return (
      <div>
        <DataTable id="rule-list"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey="title"
                   rows={this.props.rules}
                   filterBy="Title"
                   dataRowFormatter={this._ruleInfoFormatter}
                   filterLabel="Filter Rules"
                   filterKeys={filterKeys}>
          <ButtonToolbar className="pull-right">
            <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
              <Button bsStyle="success">Create Rule</Button>
            </LinkContainer>
            <DropdownButton key="more-rule-actions-button"
                            title="More actions"
                            pullRight
                            id="more-rule-actions-dropdown">
              <MenuItem onSelect={this.toggleMetricsConfig}>Configure Metrics</MenuItem>
            </DropdownButton>
          </ButtonToolbar>
        </DataTable>
        {openMetricsConfig && <RuleMetricsConfigContainer onClose={this.toggleMetricsConfig} />}
      </div>
    );
  }
}

export default RuleList;
