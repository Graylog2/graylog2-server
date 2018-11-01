import React from 'react';
import PropTypes from 'prop-types';
import { Tab, Tabs } from 'react-bootstrap';

import QueryTabActions from 'enterprise/components/QueryTabActions';
import QueryTitle from 'enterprise/components/queries/QueryTitle';
import OverviewTab from './dashboard/OverviewTab';

const QueryTabs = ({ children, onSelect, onRemove, onTitleChange, queries, selectedQuery, titles, onSaveView }) => {
  const queryTitles = titles;
  const queryTabs = queries.map((id, index) => {
    const title = queryTitles.get(id, `Query#${index + 1}`);
    const tabTitle = <QueryTitle value={title} onChange={newTitle => onTitleChange(id, newTitle)} onClose={() => onRemove(id)} />;
    return (
      <Tab key={id}
           eventKey={id}
           title={tabTitle}
           mountOnEnter
           unmountOnExit>
        {children}
      </Tab>
    );
  });
  const newTab = <Tab key="new" eventKey="new" title="+" />;

  const tabs = [queryTabs, newTab];

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
  children: PropTypes.oneOfType([PropTypes.element, PropTypes.arrayOf(PropTypes.element)]).isRequired,
  onSaveView: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQuery: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

QueryTabs.defaultProps = {
  results: {},
};

export default QueryTabs;
