import React, { useEffect } from 'react';
import styled from 'styled-components';
import ClipboardJS from 'clipboard';
import { readableColor } from 'polished';

import { teinte } from 'theme';
import { OverlayTrigger, Tooltip } from 'components/graylog';

const Name = styled.div`
  flex: 1;
  font-weight: bold;
`;

const Value = styled.div`
  text-align: right;
  opacity: 0.5;
  transition: opacity 150ms ease-in-out;
`;

const Section = styled.h3`
  margin: 0 0 6px;
`;

const Swatches = styled.div`
  display: flex;
  margin: 0 0 15px;
`;

const Swatch = styled.div(({ color }) => `
  height: 60px;
  background-color: ${color};
  border: 1px solid #222;
  color: ${readableColor(color)};
  display: flex;
  padding: 3px;
  flex-direction: column;
  cursor: pointer;
  margin-right: 6px;
  flex: 1;

  &:last-of-type {
    margin: 0;
  }

  &:hover {
    ${Value} {
      opacity: 1;
    }
  }
`);

const Colors = () => {
  let clipboard;
  const handleCopySuccess = () => {

  };

  if (ClipboardJS.isSupported()) {
    useEffect(() => {
      clipboard = new ClipboardJS('[data-clipboard-button]', {});

      clipboard.on('success', handleCopySuccess);

      return () => {
        clipboard.destroy();
      };
    }, []);
  }


  return (
    <div>
      {Object.keys(teinte).map((section) => {
        return (
          <>
            <Section>{section}</Section>

            <Swatches>
              {Object.keys(teinte[section]).map((color) => {
                const name = `teinte.${section}.${color}`;
                const value = teinte[section][color];

                return (
                  <OverlayTrigger placement="top"
                                  overlay={<Tooltip id={`tooltip-teinte-${section}-${color}`}>Copied!</Tooltip>}
                                  trigger="click"
                                  delayShow={300}
                                  delayHide={150}>
                    <Swatch color={value}
                            data-clipboard-button
                            data-clipboard-text={name}>
                      <Name>{color}</Name>
                      <Value>{value}</Value>
                    </Swatch>
                  </OverlayTrigger>
                );
              })}
            </Swatches>
          </>
        );
      })}
    </div>
  );
};

export default Colors;
