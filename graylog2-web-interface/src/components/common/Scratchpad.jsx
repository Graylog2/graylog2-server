import React, { useContext, useState, useEffect, useRef } from 'react';
import styled from 'styled-components';

import teinte from 'theme/teinte';
import { Alert } from 'components/graylog';
import { Interactable } from 'components/common';
import isLocalStorageReady from 'util/isLocalStorageReady';
import { ScratchpadContext } from 'routing/context/ScratchpadProvider';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';
const DEFAULT_SCRATCHDATA = '';

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Description = styled.p`
  color: ${teinte.primary.due};
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
  const [size, setSize] = useState((storage && storage.size) || undefined);
  const [position, setPosition] = useState((storage && storage.position) || undefined);

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

  const handleDrag = (newPosition) => {
    setPosition(newPosition);
    writeData({ position: newPosition });
  };

  const handleSize = (newSize) => {
    setSize(newSize);
    writeData({ size: newSize });
  };

  useEffect(() => {
    if (textareaRef.current && isScratchpadVisible) {
      textareaRef.current.focus();
    }
  }, [isScratchpadVisible]);

  if (!isScratchpadVisible) return null;

  return (
    <Interactable title="Scratchpad"
                  onClose={() => setScratchpadVisibility(false)}
                  onDrag={handleDrag}
                  onResize={handleSize}
                  size={size}
                  position={position}>
      <ContentArea>
        <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

        {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

        <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
      </ContentArea>

    </Interactable>
  );
};

export default Scratchpad;
