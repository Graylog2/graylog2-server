// @flow strict
import React, { useState } from 'react';
import { Map } from 'immutable';
import { ListGroup, ListGroupItem } from 'components/graylog';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import { useStore } from 'stores/connect';
import QueryTitle from 'views/logic/queries/QueryTitle';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';

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
  return queryIds.map((queryId) => {
    const tabTitle = QueryTitle(view, queryId) || 'Unknown Page title';
    return ({ id: queryId, name: tabTitle });
  });
};

const MoveWidgetToTabModal = ({ view, onCancel, onSubmit, widgetId }: Props) => {
  const [selectedTab, setSelectedTab] = useState(null);
  const [keepCopy, setKeepCopy] = useState(false);
  const { id: activeQuery } = useStore(CurrentQueryStore);

  const list = _tabList(view).filter(({ id }) => id !== activeQuery);

  const tabList = list.map(({ id, name }) => (
    <ListGroupItem onClick={() => setSelectedTab(id)}
                   active={id === selectedTab}
                   key={id}>
      {name}
    </ListGroupItem>
  ));
  const renderResult = list && list.length > 0
    ? <ListGroup>{tabList}</ListGroup>
    : <span>No dashboards found</span>;

  return (
    <BootstrapModalForm show
                        onCancel={onCancel}
                        onSubmitForm={() => onSubmit(widgetId, selectedTab, keepCopy)}
                        title="Choose Target Page">
      {renderResult}
      <Input type="checkbox"
             id="keepCopy"
             name="keepCopy"
             label="Keep Copy on this Page"
             onChange={(e) => setKeepCopy(e.target.checked)}
             help="When 'Keep Copy on the Page' is enabled, the widget will be copied and not moved to another page"
             checked={keepCopy} />
    </BootstrapModalForm>
  );
};

export default MoveWidgetToTabModal;
