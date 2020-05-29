// @flow strict
import React, { useState } from 'react';
import { Map } from 'immutable';
import { Button, ListGroup, ListGroupItem, Modal } from 'components/graylog';
import Input from 'components/bootstrap/Input';

import View from 'views/logic/views/View';

type Props = {
  view: View,
  widgetId: string,
  onCancel: () => void,
  onSubmit: (widgetId: string, selectedTab: ?string, keepCopy: boolean) => void,
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
  const [keepCopy, setKeepCopy] = useState(false);

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
        <Input type="checkbox"
               id="keepCopy"
               name="keepCopy"
               label="Keep Copy on this Page"
               onChange={(e) => setKeepCopy(e.target.checked)}
               help="When 'Keep Copy on the Page' is enabled, the widget will be copied and not moved to another page"
               checked={keepCopy} />
      </Modal.Body>
      <Modal.Footer>
        <Button bsStyle="primary"
                disabled={selectedTab === null}
                onClick={() => onSubmit(widgetId, selectedTab, keepCopy)}>
          Select
        </Button>
        <Button onClick={onCancel}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

export default MoveWidgetToTab;
