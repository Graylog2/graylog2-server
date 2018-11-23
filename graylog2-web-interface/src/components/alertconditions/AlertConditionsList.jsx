import PropTypes from 'prop-types';
import React from 'react';

import { AlertCondition } from 'components/alertconditions';
import { EntityList, PaginatedList } from 'components/common';

class AlertConditionsList extends React.Component {
  static propTypes = {
    alertConditions: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    onConditionUpdate: PropTypes.func,
    onConditionDelete: PropTypes.func,
    isStreamView: PropTypes.bool,
  };

  static defaultProps = {
    onConditionUpdate: () => {},
    onConditionDelete: () => {},
    isStreamView: false,
  };

  state = {
    currentPage: 0,
  };

  PAGE_SIZE = 10;

  _onChangePaginatedList = (currentPage) => {
    this.setState({ currentPage: currentPage - 1 });
  };

  _paginatedConditions = () => {
    return this.props.alertConditions.slice(this.state.currentPage * this.PAGE_SIZE, (this.state.currentPage + 1) * this.PAGE_SIZE);
  };

  _formatCondition = (condition) => {
    const stream = this.props.streams.find(s => s.alert_conditions.find(c => c.id === condition.id));
    return (
      <AlertCondition key={condition.id}
                      alertCondition={condition}
                      stream={stream}
                      onUpdate={this.props.onConditionUpdate}
                      onDelete={this.props.onConditionDelete}
                      isStreamView={this.props.isStreamView} />
    );
  };

  render() {
    const alertConditions = this.props.alertConditions;

    return (
      <PaginatedList totalItems={alertConditions.length}
                     onChange={this._onChangePaginatedList}
                     showPageSizeSelect={false}
                     pageSize={this.PAGE_SIZE}>
        <EntityList bsNoItemsStyle="info"
                    noItemsText="There are no configured conditions."
                    items={this._paginatedConditions().map(condition => this._formatCondition(condition))} />
      </PaginatedList>
    );
  }
}

export default AlertConditionsList;
