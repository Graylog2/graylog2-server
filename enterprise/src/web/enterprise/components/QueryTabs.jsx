import React from 'react';
import PropTypes from 'prop-types';
import { Tab, Tabs } from 'react-bootstrap';

import QueryTabActions from 'enterprise/components/QueryTabActions';
import QueryTitle from 'enterprise/components/queries/QueryTitle';

const QueryTabs = ({ children, onSelect, onRemove, onTitleChange, queries, selectedQuery, titles, onSaveView, renderDashboardTab }) => {
  const queryTitles = titles;
  const tabs = queries.toArray().map((query, index) => {
    const id = query.get('id');
    const title = queryTitles.getIn([id, 'tab', 'title'], `Query#${index + 1}`);
    const tabTitle = <QueryTitle value={title} onChange={newTitle => onTitleChange(id, newTitle)} onClose={() => onRemove(id)} />;
    return (
      <Tab key={id}
           eventKey={id}
           title={tabTitle}>
        {id === selectedQuery && children(query, index)}
      </Tab>
    );
  });
  tabs.push(<Tab key="new" eventKey="new" title="+" />);
  tabs.push(<Tab key="dashboard" eventKey="dashboard" title="Dashboard" mountOnEnter>{renderDashboardTab()}</Tab>);

  return (
    <span>
      <span className="pull-right">
        <QueryTabActions onSaveView={onSaveView} />
      </span>
      <Tabs
        id="QueryTabs"
        activeKey={selectedQuery}
        animation={false}
        onSelect={onSelect}>
        {tabs}
      </Tabs>
    </span>
  );
};

QueryTabs.propTypes = {
  children: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQuery: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
  renderDashboardTab: PropTypes.func,
};

QueryTabs.defaultProps = {
  results: {},
  renderDashboardTab: () => null,
};

export default QueryTabs;
