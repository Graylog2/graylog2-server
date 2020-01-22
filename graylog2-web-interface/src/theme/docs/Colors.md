_Click any color block below to copy the color path._

```js noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';

import { color } from 'theme';
import ColorSwatch from './Colors';

const Modes = styled.div`
  margin: 0 0 60px;
`;
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
  return Object.keys(data).map((key, index) => callback(key, index));
}

const SectionWrap = (mode, section) => {
  return (
    <>
      <Swatches>
        {getValues(mode, (name, index) =>
          typeof mode[name] === 'string' && (
            <StyledColorSwatch name={name}
                               color={mode[name]}
                               key={`${index}.${section}.${name}`}
                               copyText={`theme.color.${section}.${name}`} />
          )
        )}
      </Swatches>

      <div>
        {getValues(mode, (name) =>
          typeof mode[name] === 'object' && (
            <>
              <Section>{section} &mdash; {name}</Section>

              <Swatches>
                {getValues(mode[name], (subname, index) => (
                  <StyledColorSwatch name={subname}
                                      color={mode[name][subname]}
                                      key={`${index}.${section}.${name}`}
                                      copyText={`theme.color.${section}.${name}.${subname}`} />
                ))}
              </Swatches>
            </>
          )
        )}
      </div>
    </>
  );
};

const Colors = () => {
  return (
    <>
      {getValues(color, (mode) => (
          <Modes>
            <Mode>{mode}</Mode>

            {getValues(color[mode], (section) => (
              <>
                <Section>{section}</Section>
                {SectionWrap(color[mode][section], section)}
              </>
            ))}
          </Modes>
        )
      )}
    </>
  );
};

<Colors />
```
