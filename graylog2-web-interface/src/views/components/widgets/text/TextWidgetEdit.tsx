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
import { useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import debounce from 'lodash/debounce';

import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import MDBaseEditor from 'components/common/MarkdownEditor/BaseEditor';
import Preview from 'components/common/MarkdownEditor/Preview';
import type { EditWidgetComponentProps } from 'views/types';
import FullSizeContainer from 'views/components/aggregationbuilder/FullSizeContainer';

const EditorContainer = styled.div`
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 5px;
`;
const Item = styled.div`
  flex: 1;
  height: 100%;
`;
const TextWidgetEdit = ({ config, onChange }: EditWidgetComponentProps<TextWidgetConfig>) => {
  const [text, setText] = useState(config?.text ?? '');
  const _onChange = useMemo(
    () => debounce((newText: string) => onChange(new TextWidgetConfig(newText)), 200),
    [onChange],
  );

  useEffect(() => {
    _onChange(text);
  }, [_onChange, text]);

  return (
    <FullSizeContainer>
      {({ height }) => (
        <EditorContainer>
          <Item>
            <MDBaseEditor
              key={`markdown-editor-${height}`}
              autoFocus
              onChange={setText}
              value={text}
              height={height - 8}
            />
          </Item>
          <Item>
            <Preview value={text} height={height - 8} show />
          </Item>
        </EditorContainer>
      )}
    </FullSizeContainer>
  );
};
export default TextWidgetEdit;
