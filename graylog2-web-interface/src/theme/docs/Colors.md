_Click any color block below to copy the color path._

```js noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';

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
  return (
    <div>
      {Object.keys(teinte).map((section) => (
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
        )
      )}
    </div>
  );
};

<Colors />
```
