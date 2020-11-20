/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import React, { useState, useCallback } from 'react';

import { ListGroup, ListGroupItem } from 'components/graylog';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import { useStore } from 'stores/connect';
import QueryTitle from 'views/logic/queries/QueryTitle';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { QueryIdsStore } from 'views/stores/QueryIdsStore';
import View from 'views/logic/views/View';

type Props = {
  view: View,
  widgetId: string,
  onCancel: () => void,
  onSubmit: (widgetId: string, selectedTab: ?string, keepCopy: boolean) => void,
};

type TabEntry = { id: string, name: string };

const _tabList = (view: View, queryIds): Array<TabEntry> => {
  return queryIds.map((queryId) => {
    const tabTitle = QueryTitle(view, queryId) || 'Unknown Page title';

    return ({ id: queryId, name: tabTitle });
  });
};

const MoveWidgetToTabModal = ({ view, onCancel, onSubmit, widgetId }: Props) => {
  const [selectedTab, setSelectedTab] = useState(null);
  const [keepCopy, setKeepCopy] = useState(false);
  const { id: activeQuery } = useStore(CurrentQueryStore);
  const queryIds = useStore(QueryIdsStore);
  const onKeepCopy = useCallback((e) => setKeepCopy(e.target.checked), [setKeepCopy]);
  const submit = useCallback(() => onSubmit(widgetId, selectedTab, keepCopy), [widgetId, selectedTab, keepCopy]);

  const list = _tabList(view, queryIds.toArray()).filter(({ id }) => id !== activeQuery);

  const tabList = list.map(({ id, name }) => (
    <ListGroupItem onClick={() => setSelectedTab(id)}
                   active={id === selectedTab}
                   key={id}>
      {name}
    </ListGroupItem>
  ));
  const renderResult = list && list.length > 0
    ? <ListGroup>{tabList}</ListGroup>
    : <span>No pages found</span>;

  return (
    <BootstrapModalForm show
                        onCancel={onCancel}
                        submitButtonDisabled={!selectedTab}
                        onSubmitForm={submit}
                        title="Choose Target Page">
      {renderResult}
      <Input type="checkbox"
             id="keepCopy"
             name="keepCopy"
             label="Keep Copy on this Page"
             onChange={onKeepCopy}
             help="When 'Keep Copy on the Page' is enabled, the widget will be copied and not moved to another page"
             checked={keepCopy} />
    </BootstrapModalForm>
  );
};

export default MoveWidgetToTabModal;
