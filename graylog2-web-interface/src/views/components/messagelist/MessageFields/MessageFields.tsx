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
import StringUtils from 'util/StringUtils';
import useSendFavoriteFieldTelemetry from 'views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry';

const Line = styled.div(
  ({ theme }) => css`
    border-top: 1px ${theme.colors.table.row.divider} solid;
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
    padding: ${theme.spacings.xxs} 0;
    font-size: ${theme.fonts.size.small};
    gap: ${theme.spacings.xs};
  `,
);

type Props = {
  onClick: () => void;
  expanded: boolean;
  restLength: number;
};
const IconSeparator = ({ expanded }: { expanded: boolean }) => (
  <Icon size="xs" name={expanded ? 'collapse_all' : 'expand_all'} type="regular" />
);
const Separator = ({ onClick, expanded, restLength }: Props) => {
  const onKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        onClick();
      }
    },
    [onClick],
  );

  return (
    <SeparatorContainer onClick={onClick} role="button" tabIndex={0} onKeyDown={onKeyDown}>
      <Line />
      <IconSeparator expanded={expanded} />
      <span>{`${expanded ? 'Hide' : 'Show'} ${restLength || ''} remaining ${StringUtils.pluralize(restLength, 'field', 'fields')}`}</span>
      <IconSeparator expanded={expanded} />
      <Line />
    </SeparatorContainer>
  );
};

const SESSION_STORAGE_KEY = 'message_table_show_rest-fields';

const MessageFields = () => {
  const sendFavoriteFieldTelemetry = useSendFavoriteFieldTelemetry();
  const [expanded, setExpanded] = useState(Store.sessionGet(SESSION_STORAGE_KEY) !== 'false');
  const { favoriteFields } = useContext(MessageFavoriteFieldsContext);
  const { formattedFavorites, formattedRest } = useFormattedFields(favoriteFields);
  const toggleExpand = useCallback(() => {
    const isNowExpanded = !expanded;
    if (isNowExpanded) {
      Store.sessionDelete(SESSION_STORAGE_KEY);
      setExpanded(true);
    } else {
      Store.sessionSet(SESSION_STORAGE_KEY, 'false');
      setExpanded(false);
    }
    sendFavoriteFieldTelemetry('NON_FAVORITE_SHOW_TOGGLED', {
      is_expanded: isNowExpanded,
    });
  }, [expanded, sendFavoriteFieldTelemetry]);

  const hasFavorites = !!formattedFavorites?.length;

  return (
    <>
      <MessageFieldsViewModeList fields={formattedFavorites} isFavorite />
      {hasFavorites && <Separator onClick={toggleExpand} expanded={expanded} restLength={formattedRest?.length} />}
      {(expanded || !hasFavorites) && <MessageFieldsViewModeList fields={formattedRest} />}
    </>
  );
};

export default MessageFields;
