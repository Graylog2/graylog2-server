import React from 'react';
import PropTypes from 'prop-types';
import { Tab, Tabs } from 'react-bootstrap';

import ViewActionsMenu from 'views/components/ViewActionsMenu';
import QueryTitle from 'views/components/queries/QueryTitle';

const QueryTabs = ({ children, onSelect, onRemove, onTitleChange, queries, selectedQuery, titles, onSaveView, onSaveAsView }) => {
  const queryTitles = titles;
  const queryTabs = queries.map((id, index) => {
    const title = queryTitles.get(id, `Query#${index + 1}`);
    const tabTitle = (
      <QueryTitle value={title}
                  id={id}
                  active={id === selectedQuery}
                  onChange={newTitle => onTitleChange(id, newTitle)}
                  onClose={() => onRemove(id)} />
    );
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
        <ViewActionsMenu onSaveView={onSaveView} onSaveAsView={onSaveAsView} />
      </span>
      <Tabs id="QueryTabs"
            activeKey={selectedQuery}
            animation={false}
            onSelect={onSelect}>
        {tabs}
      </Tabs>
    </span>
  );
};

QueryTabs.propTypes = {
  children: PropTypes.node,
  onSaveView: PropTypes.func.isRequired,
  onSaveAsView: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQuery: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

QueryTabs.defaultProps = {
  children: null,
};

export default QueryTabs;
