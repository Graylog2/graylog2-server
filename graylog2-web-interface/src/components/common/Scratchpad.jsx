import React, { useContext, useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
import ReactDOM from 'react-dom';

import { Alert } from 'components/graylog';
import { Interactable } from 'components/common';
import isLocalStorageReady from 'util/isLocalStorageReady';
import { ScratchpadContext } from 'routing/context/ScratchpadProvider';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';
const DEFAULT_SCRATCHDATA = '';
const DEFAULT_SIZE = { width: 450, height: 300 };
const DEFAULT_POSITION = { x: window.document.body.offsetWidth - 250, y: 225 };

const ScratchpadWrapper = styled.div`
  position: fixed;
  pointer-events: none;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
`;

const StyledInteractable = styled(Interactable)`
  box-shadow: 0 0 9px rgba(31, 31, 31, .25),
              0 0 6px rgba(31, 31, 31, .25),
              0 0 3px rgba(31, 31, 31, .25);
  background-color: #393939;
  border-radius: 3px;
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
  const storage = JSON.parse(localStorage.getItem(LOCALSTORAGE_ITEM));
  const textareaRef = useRef();
  const { isScratchpadVisible, setScratchpadVisibility } = useContext(ScratchpadContext);
  const [scratchData, setScratchData] = useState((storage && storage.value) || DEFAULT_SCRATCHDATA);
  const [localStorageReady] = useState(isLocalStorageReady());
  const [size, setSize] = useState((storage && storage.size) || DEFAULT_SIZE);
  const [position, setPosition] = useState((storage && storage.position) || DEFAULT_POSITION);

  const writeData = (newData) => {
    if (localStorageReady) {
      localStorage.setItem(LOCALSTORAGE_ITEM, JSON.stringify({ ...storage, ...newData }));
    }
  };

  const handleChange = () => {
    const { value } = textareaRef.current;

    setScratchData(value);
    writeData({ value: textareaRef.current.value });
  };

  const handleInteractable = ({ size: newSize, position: newPosition }) => {
    setSize(newSize);
    setPosition(newPosition);
    writeData({ size: newSize, position: newPosition });
  };

  useEffect(() => {
    if (textareaRef.current && isScratchpadVisible) {
      textareaRef.current.focus();
    }
  }, [isScratchpadVisible]);

  if (!isScratchpadVisible) return null;

  return ReactDOM.createPortal(
    (
      <ScratchpadWrapper>
        <StyledInteractable isDraggable
                            isResizable
                            size={size}
                            minHeight={250}
                            minWidth={250}
                            position={position}
                            onResizeEnd={handleInteractable}
                            onDragEnd={handleInteractable}>

          <ContentArea>
            <Title>Scratchpad <ToggleButton type="button" onClick={() => setScratchpadVisibility(false)}><i className="fa fa-times" /></ToggleButton></Title>
            <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

            {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

            <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
          </ContentArea>

        </StyledInteractable>
      </ScratchpadWrapper>
    ),
    document.body,
  );
};

export default Scratchpad;
