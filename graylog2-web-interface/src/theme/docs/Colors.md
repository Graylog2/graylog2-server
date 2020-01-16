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

const subSwatches = ({mode, section, colorValue}) => {
  console.log(mode, section, colorValue, Object.keys(colorValue));

  return (
    <Swatches>
      {Object.keys(colorValue).map((subsection) => {
        const subValue = colorValue[subsection];
        return null;

        return (
          <>
            <Section>{subsection}</Section>

            <StyledColorSwatch name={name}
                                color={subValue.toUpperCase()}
                                copyText={`theme.color.${section}.${name}.${subsection}`} />
          </>
        );
      })}
    </Swatches>
  )
}

const Colors = () => {
  return (
    <>
      {Object.keys(color).map((mode) => (
          <>
            <Mode>{mode}</Mode>

            {Object.keys(color[mode]).map((section) => {
              let colorValue;

              return (
              <>
                <Section>{section}</Section>

                <Swatches>
                  {Object.keys(color[mode][section]).map((name) => {
                    colorValue = color[mode][section][name];

                    if (typeof colorValue === 'string') {
                      return (<StyledColorSwatch name={name}
                                                 color={colorValue.toUpperCase()}
                                                 copyText={`theme.color.${section}.${name}`} />);
                    }

                    return (
                      <div>
                        {
                          Object.keys(colorValue).map((subName) => (
                            <div>
                              <Section>{section} {name}</Section>

                              <StyledColorSwatch name={subName}
                                                  color={colorValue[subName].toUpperCase()}
                                                  copyText={`theme.color.${section}.${name}.${subName}`} />
                            </div>
                          ))
                        }
                      </div>
                    )


                  })}
                </Swatches>
              </>
            )})}
          </>
        )
      )}
    </>
  );
};

<Colors />
```
