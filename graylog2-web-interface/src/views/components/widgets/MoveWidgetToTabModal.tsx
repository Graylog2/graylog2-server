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
import React, { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import { ListGroup, ListGroupItem } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import QueryTitle from 'views/logic/queries/QueryTitle';
import type View from 'views/logic/views/View';
import useQueryIds from 'views/hooks/useQueryIds';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

type Props = {
  view: View;
  widgetId: string;
  onCancel: () => void;
  onSubmit: (widgetId: string, selectedTab: string | undefined | null, keepCopy: boolean) => void;
};

type TabEntry = { id: string; name: string };

const _tabList = (view: View, queryIds): Array<TabEntry> =>
  queryIds.map((queryId) => {
    const tabTitle = QueryTitle(view, queryId) || 'Unknown Page title';

    return { id: queryId, name: tabTitle };
  });

const MoveWidgetToTabModal = ({ view, onCancel, onSubmit, widgetId }: Props) => {
  const { t } = useTranslation('widgets');
  const [selectedTab, setSelectedTab] = useState(null);
  const [keepCopy, setKeepCopy] = useState(false);
  const activeQuery = useActiveQueryId();
  const queryIds = useQueryIds();
  const onKeepCopy = useCallback((e) => setKeepCopy(e.target.checked), [setKeepCopy]);
  const submit = useCallback(
    () => onSubmit(widgetId, selectedTab, keepCopy),
    [onSubmit, widgetId, selectedTab, keepCopy],
  );

  const list = _tabList(view, queryIds.toArray()).filter(({ id }) => id !== activeQuery);

  const tabList = list.map(({ id, name }) => (
    <ListGroupItem onClick={() => setSelectedTab(id)} active={id === selectedTab} key={id}>
      {name}
    </ListGroupItem>
  ));
  const renderResult =
    list && list.length > 0 ? <ListGroup>{tabList}</ListGroup> : <span>{t('move_widget.no_pages')}</span>;

  return (
    <BootstrapModalForm
      show
      onCancel={onCancel}
      submitButtonDisabled={!selectedTab}
      submitButtonText={keepCopy ? t('move_widget.copy') : t('move_widget.move')}
      onSubmitForm={submit}
      title={t('move_widget.title')}>
      {renderResult}
      <Input
        type="checkbox"
        id="keepCopy"
        name="keepCopy"
        label={t('move_widget.keep_copy')}
        onChange={onKeepCopy}
        help={t('move_widget.keep_copy_help')}
        checked={keepCopy}
      />
    </BootstrapModalForm>
  );
};

export default MoveWidgetToTabModal;
