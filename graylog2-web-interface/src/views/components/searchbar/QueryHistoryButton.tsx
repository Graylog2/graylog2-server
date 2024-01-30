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
import * as React from 'react';
import styled from 'styled-components';

import { SearchSuggestions } from '@graylog/server-api';
import IconButton from 'components/common/IconButton';
import type { Editor } from 'views/components/searchbar/queryinput/ace-types';
import useHotkey from 'hooks/useHotkey';
import { startAutocomplete } from 'views/components/searchbar/queryinput/commands';

const QUERY_HISTORY_LIMIT = 100;

const ButtonContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-left: 6px;
`;

export const fetchQueryHistory = () => SearchSuggestions.suggestQueryStrings(QUERY_HISTORY_LIMIT).then((response) => (
  response.sort((
    { last_used: lastUsedA }, { last_used: lastUsedB }) => new Date(lastUsedB).getTime() - new Date(lastUsedA).getTime(),
  ).map((entry, index) => ({
    value: entry.query,
    meta: 'history',
    score: index,
    completer: {
      insertMatch: (editor: { setValue: (value: string) => void }, data) => {
        editor.setValue(data.value);
      },
    },
  }))));

type Props = {
  editorRef: React.MutableRefObject<Editor>
}

const QueryHistoryButton = ({ editorRef }: Props) => {
  const showQueryHistory = async () => {
    if (editorRef.current) {
      editorRef.current.focus();

      startAutocomplete(editorRef.current, { matches: await fetchQueryHistory() });
    }
  };

  useHotkey({
    actionKey: 'show-query-history',
    callback: () => showQueryHistory(),
    scope: 'search',
  });

  return (
    <ButtonContainer>
      <IconButton name="clock-rotate-left" onClick={showQueryHistory} />
    </ButtonContainer>
  );
};

export default QueryHistoryButton;
