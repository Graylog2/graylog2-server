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
import styled, { css } from 'styled-components';

import { SourceCodeEditor, Icon } from 'components/common';

import Preview from './Preview';
import EditorModal from './EditorModal';

const TabsRow = styled.div`
  position: relative;
  bottom: -1px;
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  gap: 5px;
  padding: 0 8px;
  z-index: 1;
`;

const Tab = styled.div<{ $active?: boolean }>`
  padding: 4px 8px;
  border: none;
  border-bottom: 1px solid ${({ theme }) => theme.colors.input.border};
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  background-color: ${({ theme }) => theme.colors.global.contentBackground};
  color: ${({ theme, $active }) => ($active ? theme.colors.global.textDefault : theme.colors.input.placeholder)};
  cursor: pointer;

  ${({ $active }) => $active && css`
    border: 1px solid ${({ theme }) => theme.colors.input.border};
    border-bottom-color: ${({ theme }) => theme.colors.global.contentBackground};
  `}
`;

const EditorStyles = styled.div`
  & .ace_editor {
    border-color: ${({ theme }) => theme.colors.input.border} !important;
  }

  & .ace_cursor {
      border-color: ${({ theme }) => theme.colors.global.textDefault};
  }
`;

const ExpandIcon = styled(Icon)`
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.colors.input.placeholder};
  z-index: 10;

  &:hover {
    color: ${({ theme }) => theme.colors.global.textDefault};
  }
`;

type Props = {
  id?: string;
  value: string;
  height: number;
  readOnly?: boolean;
  onChange: (note: string) => void;
  onFullMode?: (fullMode: boolean) => void;
}

function Editor({ id, value, height, readOnly = false, onChange, onFullMode }: Props) {
  const [localValue, setLocalValue] = React.useState<string>(value);
  const [showPreview, setShowPreview] = React.useState<boolean>(false);
  const [fullView, setFullView] = React.useState<boolean>(false);

  React.useEffect(() => setLocalValue(value), [value]);

  const handleOnFullMode = (fullMode: boolean) => {
    setFullView(fullMode);
    if (onFullMode) onFullMode(fullMode);
  };

  const handleOnChange = (newValue: string) => {
    setLocalValue(newValue);
    onChange(newValue);
  };

  return (
    <>
      <div style={{ position: 'relative' }}>
        <TabsRow>
          <Tab $active={!showPreview} onClick={() => setShowPreview(false)}>Edit</Tab>
          <Tab $active={showPreview} onClick={() => setShowPreview(true)}>Preview</Tab>
        </TabsRow>
        {!showPreview && (
          <EditorStyles>
            <SourceCodeEditor id={id ?? 'md-editor'}
                              mode="markdown"
                              theme="light"
                              toolbar={false}
                              resizable={false}
                              readOnly={readOnly}
                              height={height}
                              value={localValue}
                              onChange={handleOnChange} />
          </EditorStyles>
        )}
        <Preview value={localValue} height={height} show={showPreview} />
        <ExpandIcon data-testid="expand-icon" name="expand" onClick={() => handleOnFullMode(true)} />
      </div>
      {fullView && (
        <EditorModal value={localValue}
                     readOnly={readOnly}
                     show={fullView}
                     onChange={handleOnChange}
                     onClose={() => handleOnFullMode(false)} />
      )}
    </>
  );
}

export default Editor;
