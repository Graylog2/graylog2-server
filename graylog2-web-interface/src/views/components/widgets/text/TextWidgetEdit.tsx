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
    () => debounce((newText: string) => onChange(new TextWidgetConfig(newText)), 50),
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
