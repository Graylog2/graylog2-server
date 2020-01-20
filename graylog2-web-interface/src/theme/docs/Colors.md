_Click any color block below to copy the color path._

```js noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';

import { color } from 'theme';
import ColorSwatch from './Colors';

const Mode = styled.h3`
  margin: 0 0 6px;
`;

const Section = styled.h4`
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

const getValues = (data = {}, callback = () => {}) => {
  return Object.keys(data).map((key) => callback(key));
}

const ColorSwatches = (mode, section) => {
  const swatches = [];

  Object.keys(color[mode][section]).map((name) => {
    const colorValue = color[mode][section][name];

    swatches.push(
      <div>
        {typeof colorValue === 'string'
          ? (
            <>
              <Section>{name}</Section>

              <StyledColorSwatch name={name}
                                color={colorValue.toUpperCase()}
                                copyText={`theme.color.${section}.${name}`} />

            </>)
          : (<div>
              <Section>{section} {name}</Section>

              <Swatches>
                {Object.keys(colorValue).map((subName) => (
                  <StyledColorSwatch name={subName}
                                    color={colorValue[subName].toUpperCase()}
                                    copyText={`theme.color.${section}.${name}.${subName}`} />
                ))}
              </Swatches>
            </div>)
        }
      </div>
    );
  });

  return swatches;
};

const Colors = () => {
  return (
    <>
      {Object.keys(color).map((mode) => (
          <>
            <Mode>{mode}</Mode>

            {Object.keys(color[mode]).map((section) => ColorSwatches(mode, section))}
          </>
        )
      )}
    </>
  );
};

<Colors />
```
