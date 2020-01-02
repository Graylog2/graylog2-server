_Click any color block below to copy the color path._

```js noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';
import ClipboardJS from 'clipboard';
import { readableColor } from 'polished';

import { teinte } from 'theme';
import ColorSwatch from './Colors';

const Section = styled.h3`
  margin: 0 0 6px;
`;

const Swatches = styled.div`
  display: flex;
  margin: 0 0 15px;
`;

const StyledColorSwatch = styled(ColorSwatch)`
  margin-right: 6px;

  &:last-of-type {
    margin: 0;
  }
`;

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
              {Object.keys(teinte[section]).map((name) => {
                const value = teinte[section][name];

                return (<StyledColorSwatch name={name}
                                           color={value}
                                           copyText={`teinte.${section}.${name}`} />);
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
