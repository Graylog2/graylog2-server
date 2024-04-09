import * as React from 'react';
import ReactDom from 'react-dom';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon, SourceCodeEditor } from 'components/common';

import Preview from './Preview';

const Backdrop = styled.div`
  position: fixed;
  display: flex;
  flex-direction: column;
  align-items: center;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1051;

  background-color: rgba(0, 0, 0, 0.5);
`;

const Content = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  width: 75vw;
  max-width: 1000px;
  height: 65vh;
  top: 10vh;
  padding: 16px;

  background-color: ${({ theme }) => theme.colors.global.contentBackground};
  border: 1px solid ${({ theme }) => theme.colors.input.border};
  border-radius: 8px;
`;

const CloseIcon = styled(Icon)`
  margin-left: auto;
  cursor: pointer;
  color: ${({ theme }) => theme.colors.input.placeholder};

  &:hover {
    color: ${({ theme }) => theme.colors.global.textDefault};
  }
`;

const Row = styled.div`
  display: flex;
  flex-direction: row;
  align-items: stretch;
  justify-content: stretch;
  gap: 1rem;

  &#editor-body {
    flex-grow: 1;
  }
`;

const EditorWrapper = styled.div`
  .ace_editor {
    border: 1px solid ${({ theme }) => theme.colors.input.border} !important;
  }
`;

type Props = {
  value: string;
  readOnly?: boolean;
  show: boolean;
  onChange: (newValue: string) => void;
  onClose: () => void;
  onDone?: (newValue?: string) => void;
}

function EditorModal({ value, readOnly, onChange, show, onClose, onDone }: Props) {
  const [height, setHeight] = React.useState<number>(0);
  const [localValue, setLocalValue] = React.useState<string>(value);

  React.useEffect(() => {
    const contentHeight = document.getElementById('editor-body')?.scrollHeight;
    setHeight(contentHeight);
  }, []);

  React.useEffect(() => setLocalValue(value), [value]);

  const handleOnChange = React.useCallback((newValue: string) => {
    setLocalValue(newValue);
    onChange(newValue);
  }, [onChange]);

  const handleOnDone = React.useCallback(() => {
    if (onDone) onDone(localValue);
    onClose();
  }, [localValue, onClose, onDone]);

  const Component = React.useMemo(() => (
    show ? (
      <Backdrop onClick={() => onClose()}>
        <Content onClick={(e: React.BaseSyntheticEvent) => e.stopPropagation()}>
          <Row>
            <h2 style={{ marginBottom: '1rem' }}>Markdown Editor</h2>
            <CloseIcon name="close" onClick={() => onClose()} />
          </Row>
          <Row id="editor-body">
            {height > 0 && (
              <>
                <EditorWrapper style={{ width: '50%' }}>
                  {/* @ts-ignore */}
                  <SourceCodeEditor id="md-editor"
                                    mode="markdown"
                                    theme="light"
                                    toolbar={false}
                                    resizable={false}
                                    readOnly={readOnly}
                                    height={height}
                                    value={localValue}
                                    onChange={handleOnChange} />
                </EditorWrapper>
                <div style={{ width: '50%' }}>
                  <Preview show value={localValue} height={height} />
                </div>
              </>
            )}
          </Row>
          <Row style={{ justifyContent: 'flex-end', marginTop: '1rem' }}>
            <Button onClick={() => onClose()}>Cancel</Button>
            <Button bsStyle="success" onClick={handleOnDone}>Done</Button>
          </Row>
        </Content>
      </Backdrop>
    ) : null
  ), [show, height, localValue, readOnly, onClose, handleOnDone, handleOnChange]);

  return <>{ReactDom.createPortal(Component, document.body)}</>;
}

EditorModal.defaultProps = {
  readOnly: false,
  onDone: undefined,
};

export default EditorModal;
