import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
import ReactDOM from 'react-dom';

import { Alert } from 'components/graylog';
import Mops from 'components/common/Mops';

import isLocalStorageReady from 'util/isLocalStorageReady';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';

const ScratchpadWrapper = styled.div`
  position: fixed;
  pointer-events: none;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
`;

const ContentArea = styled.div`
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 15px;
`;

const ToggleButton = styled.button`
  background: transparent;
  border: 0;
  padding: 0;
  color: #F1F2F2;
`;

const Title = styled.h3`
  color: #fff;
  margin: 9px 0;

  > ${ToggleButton} {
    float: right;
  }
`;

const Description = styled.p`
  color: #fff;
`;

const Textarea = styled.textarea`
  width: 100%;
  resize: none;
  flex: 1;
  margin-bottom: 15px;
`;

const StyledAlert = styled(Alert)`
  margin-bottom: 10px;
`;

const Scratchpad = () => {
  const [opened, setOpened] = useState(false);
  const [scratchData, setScratchData] = useState(localStorage.getItem(LOCALSTORAGE_ITEM));
  const [localStorageReady] = useState(isLocalStorageReady());
  const textareaRef = useRef();

  const handleChange = () => {
    const { value } = textareaRef.current;
    setScratchData(value);

    if (localStorageReady) {
      localStorage.setItem(LOCALSTORAGE_ITEM, textareaRef.current.value);
    }
  };

  const size = { width: opened ? 450 : 50, height: opened ? 300 : 50 };

  useEffect(() => {
    if (textareaRef.current && opened) {
      textareaRef.current.focus();
    }
  }, [opened]);

  return ReactDOM.createPortal(
    (
      <ScratchpadWrapper>
        <Mops opened={opened}
              isDraggable
              isResizable={opened}
              size={size}
              minHeight={50}
              minWidth={50}
              position={{ x: 25, y: 75 }}>

          {!opened ? (<ToggleButton type="button" onClick={() => setOpened(true)}><i className="fa fa-pencil fa-2x" /></ToggleButton>) : (
            <ContentArea>
              <Title>Scratchpad <ToggleButton type="button" onClick={() => setOpened(false)}><i className="fa fa-times" /></ToggleButton></Title>
              <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

              {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

              <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
            </ContentArea>
          )}

        </Mops>
      </ScratchpadWrapper>
    ),
    window.document.body,
  );
};

export default Scratchpad;
