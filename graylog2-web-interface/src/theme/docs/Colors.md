All colors are available via the ThemeProvider `theme.color` prop

_Click any color block below to copy the color path._

```jsx noeditor
import React, { useEffect } from 'react';
import styled from 'styled-components';
import chroma from 'chroma-js';

import { colors } from 'theme';
import ColorSwatch, { Swatch } from './Colors';

const ModeTitle = styled.h3`
  margin: 0 0 6px;
`;

const Section = styled.h4`
  margin: 0 0 6px;
`;

const Swatches = styled.div`
  display: flex;
  margin: 0 0 15px;
  flex-wrap: wrap;
`;

const StyledColorSwatch = styled(ColorSwatch)`
  flex-basis: ${(props) => props.section === 'global' ? '100px' : '1px'};
  max-width: ${(props) => props.section === 'global' ? '200px' : 'auto'};

  ${Swatch} {
    margin-right: 6px;
    margin-bottom: 3px;

    &:last-of-type {
      margin: 0;
    }
  }
`;

const getValues = (data = {}, callback = () => {}) => {
  return Object.keys(data).map((key) => callback(key));
}

const SectionWrap = (mode, section) => {
  return (
    <React.Fragment key={`wrap-theme.colors.${section}`}>
      <Swatches>
        {getValues(mode, (name) => {
          if (typeof mode[name] === 'string' && chroma.valid(mode[name])) {
            const copyTextName = section === 'gray' ? `${section}[${name}]` : `${section}.${name}`;

            return (
              <StyledColorSwatch name={name}
                                 section={section}
                                 color={mode[name]}
                                 key={`theme.colors.${copyTextName}`}
                                 copyText={`theme.colors.${copyTextName}`}/>
            )
          }
        })}
      </Swatches>

      <div>
        {getValues(mode, (name) =>
          typeof mode[name] === 'object' && (
            <React.Fragment key={`wrap-theme.colors.${section}.${name}`}>
              <Section>{section} &mdash; {name}</Section>

              <Swatches>
                {getValues(mode[name], (subname) => (
                  <StyledColorSwatch name={subname}
                                     section={section}
                                     color={mode[name][subname]}
                                     key={`theme.colors.${section}.${name}.${subname}`}
                                     copyText={`theme.colors.${section}.${name}.${subname}`}/>
                ))}
              </Swatches>
            </React.Fragment>
          )
        )}
      </div>
    </React.Fragment>
  );
};

const Colors = () => {
  return (
    <>
      {getValues(colors, (themeMode) => (
        <React.Fragment key={`wrap-title-${themeMode}`}>
          <ModeTitle>{themeMode}</ModeTitle>
          {getValues(colors[themeMode], (section) => (
            <>
              <Section key={`section-${section}`}>{section}</Section>
              {SectionWrap(colors[themeMode][section], section)}
            </>
          ))}
        </React.Fragment>
      ))}
    </>
  );
};

<Colors />
```
