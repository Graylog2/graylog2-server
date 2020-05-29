// @flow strict
import React, { useState } from 'react';
import { Map } from 'immutable';
import { Button, ListGroup, ListGroupItem, Modal } from 'components/graylog';

import View from 'views/logic/views/View';

type Props = {
  view: View,
  widgetId: string,
  onCancel: () => void,
  onSubmit: (widgetId: string, selectedTab: ?string) => void,
};

type TabEntry = { id: string, name: string };

const _tabList = (view: View): Array<TabEntry> => {
  const queryIds = Object.keys(view.state.toObject());
  return queryIds.map((queryId, index) => {
    const tabTitle = view.state.get(queryId).titles.get('tab', Map({ title: `Page#${index + 1}` }));
    return ({ id: queryId, name: tabTitle.get('title') });
  });
};

const MoveWidgetToTab = ({ view, onCancel, onSubmit, widgetId }: Props) => {
  const [selectedTab, setSelectedTab] = useState(null);
  const list = _tabList(view);

  const tabList = list.map(({ id, name }) => (
    <ListGroupItem header={name}
                   onClick={() => setSelectedTab(id)}
                   active={id === selectedTab}
                   key={id} />
  ));
  const renderResult = list && list.length > 0
    ? <ListGroup>{tabList}</ListGroup>
    : <span>No dashboards found</span>;

  return (
    <Modal show>
      <Modal.Body>
        {renderResult}
      </Modal.Body>
      <Modal.Footer>
        <Button bsStyle="primary"
                disabled={selectedTab === null}
                onClick={() => onSubmit(widgetId, selectedTab)}>
          Select
        </Button>
        <Button onClick={onCancel}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

export default MoveWidgetToTab;
