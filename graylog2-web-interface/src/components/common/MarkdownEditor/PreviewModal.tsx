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
import ReactDom from 'react-dom';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';

import Preview from './Preview';

const Backdrop = styled.div`
  position: fixed;
  display: flex;
  flex-direction: column;
  align-items: center;
  inset: 0;
  z-index: 1051;

  background-color: rgb(0 0 0 / 50%);
`;

const Content = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  width: 600px;
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

type Props = {
  value: string;
  show: boolean;
  onClose: () => void;
}

function PreviewModal({ value, show, onClose }: Props) {
  const [height, setHeight] = React.useState<number>(0);

  React.useEffect(() => {
    const contentHeight = document.getElementById('preview-body')?.scrollHeight;
    setHeight(contentHeight);
  }, [show]);

  const Component = React.useMemo(() => (show ? (
    <Backdrop onClick={() => onClose()}>
      <Content onClick={(e: React.BaseSyntheticEvent) => e.stopPropagation()}>
        <Row>
          <h2 style={{ marginBottom: '1rem' }}>Markdown Preview</h2>
          <CloseIcon name="close" onClick={() => onClose()} />
        </Row>
        <Preview value={value} height={height} show />
        <Row style={{ justifyContent: 'flex-end', marginTop: '1rem' }}>
          <Button bsStyle="success" role="button" onClick={() => onClose()}>Close</Button>
        </Row>
      </Content>
    </Backdrop>
  ) : null), [show, value, height, onClose]);

  return <>{ReactDom.createPortal(Component, document.body)}</>;
}

export default PreviewModal;
