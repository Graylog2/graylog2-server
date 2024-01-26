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

import IconButton from 'components/common/IconButton';
import type { Editor } from 'views/components/searchbar/queryinput/ace-types';

const ButtonContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-left: 6px;
`;

type Props = {
  editorRef: React.MutableRefObject<Editor>
}

const QueryHistoryButton = ({ editorRef }: Props) => {
  const showQueryHistory = () => {
    if (editorRef.current) {
      editorRef.current.focus();
      editorRef.current.execCommand('startAutocomplete', { context: 'showHistory' });
    }
  };

  return (
    <ButtonContainer>
      <IconButton name="clock-rotate-left" onClick={showQueryHistory} />
    </ButtonContainer>
  );
};

export default QueryHistoryButton;
