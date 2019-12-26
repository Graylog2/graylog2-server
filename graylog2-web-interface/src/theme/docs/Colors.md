_Click any color block below to copy the color path._

```js noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';
import ClipboardJS from 'clipboard';
import { readableColor } from 'polished';

import { teinte } from 'theme';

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
  useEffect(() => {
     const clipboard = new ClipboardJS('[data-clipboard-button]');

    return () => {
      clipboard.destroy();
    };
  }, []);

  return (
    <div>
      {Object.keys(teinte).map((section) => {
        return (
          <>
            <Section>{section}</Section>

            <Swatches>
              {Object.keys(teinte[section]).map((color) => {
                const value = teinte[section][color];

                return (
                  <Swatch color={value}
                          data-clipboard-text={`teinte.${section}.${color}`}
                          data-clipboard-button>
                    <Name>{color}</Name>
                    <Value>{value}</Value>
                  </Swatch>
                );
              })}
            </Swatches>
          </>
        );
      })}
    </div>
  );
};

<Colors />
```
