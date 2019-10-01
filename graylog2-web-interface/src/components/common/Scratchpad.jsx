import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import Interactable from 'components/common/Interactable';

import isLocalStorageReady from 'util/isLocalStorageReady';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';

const ScratchpadInteractable = styled(Interactable)`
  overflow: hidden;
  box-shadow: 0 0 3px rgba(0, 0, 0, .25);
  transition: width 150ms ease-in-out, height 150ms ease-in-out, border-radius 150ms ease-in-out;
  background-color: #393939;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: ${({ opened }) => (opened ? '3px' : '50%')};
`;

const ContentArea = styled.div`
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 420px; /* 450 - 30 */
  order: 2;
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

  useEffect(() => {
    if (textareaRef.current && opened) {
      textareaRef.current.focus();
    }
  }, [opened]);

  return (
    <ScratchpadInteractable opened={opened} draggable resizable={opened} width={opened ? 450 : 50} height={opened ? 300 : 50}>

      {!opened ? (<ToggleButton type="button" onClick={() => setOpened(true)}><i className="fa fa-pencil fa-2x" /></ToggleButton>) : (
        <ContentArea>
          <Title>Scratchpad <ToggleButton type="button" onClick={() => setOpened(false)}><i className="fa fa-times" /></ToggleButton></Title>
          <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

          {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

          <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
        </ContentArea>
      )}

    </ScratchpadInteractable>
  );
};

export default Scratchpad;
