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

import React, { useContext, useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import MessageFieldsViewModeList from 'views/components/messagelist/MessageFields/MessageFieldsViewModeList';
import useFormattedFields from 'views/components/messagelist/MessageFields/hooks/useFormattedFields';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import { Icon } from 'components/common';
import Store from 'logic/local-storage/Store';

const Line = styled.div(
  ({ theme }) => css`
    border-top: 1px ${theme.colors.text.secondary} solid;
    flex-grow: 1;
    height: 1px;
  `,
);
const SeparatorContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    color: ${theme.colors.text.secondary};
    cursor: pointer;
    padding: ${theme.spacings.xs} 0;
  `,
);

type Props = {
  onClick: () => void;
  expanded: boolean;
};
const Separator = ({ onClick, expanded }: Props) => (
  <SeparatorContainer onClick={onClick} title={`${expanded ? 'Hide' : 'Show'} rest fields`}>
    <Line />
    <Icon name={expanded ? 'expand_circle_up' : 'expand_circle_down'} type="regular" />
    <Line />
  </SeparatorContainer>
);

const SESSION_STORAGE_KEY = 'message_table_show_rest-fields';

const MessageFieldsViewMode = () => {
  const [expanded, setExpanded] = useState(Store.sessionGet(SESSION_STORAGE_KEY) !== 'false');
  const { favoriteFields } = useContext(MessageFavoriteFieldsContext);
  const { formattedFavorites, formattedRest } = useFormattedFields(favoriteFields);
  const toggleExpand = useCallback(() => {
    if (expanded) {
      Store.sessionSet(SESSION_STORAGE_KEY, 'false');
      setExpanded(false);
    } else {
      Store.sessionDelete(SESSION_STORAGE_KEY);
      setExpanded(true);
    }
  }, [expanded]);

  return (
    <>
      <MessageFieldsViewModeList fields={formattedFavorites} />
      <Separator onClick={toggleExpand} expanded={expanded} />
      {expanded && <MessageFieldsViewModeList fields={formattedRest} />}
    </>
  );
};

export default MessageFieldsViewMode;
