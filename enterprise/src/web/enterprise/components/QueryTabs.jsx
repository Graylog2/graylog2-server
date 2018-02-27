import React from 'react';
import PropTypes from 'prop-types';
import { Tab, Tabs } from 'react-bootstrap';
import Immutable from 'immutable';

import DebugOverlay from 'enterprise/components/DebugOverlay';
import QueryTitle from 'enterprise/components/queries/QueryTitle';

const QueryTabs = ({ children, onSelect, onRemove, onTitleChange, queries, selectedQuery, titles }) => {
  const queryTitles = titles || new Immutable.Map();
  const tabs = queries.toArray().map((query, index) => {
    const id = query.get('id');
    const title = queryTitles.get(id, `Query#${index + 1}`);
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

  return (
    <span>
      <span className="pull-right"><DebugOverlay /></span>
      <Tabs
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
};

QueryTabs.defaultProps = {
  results: {},
};

export default QueryTabs;
