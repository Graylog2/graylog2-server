All colors are available via the ThemeProvider `theme.color` prop

_Click any color block below to copy the color path._

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';
import chroma from 'chroma-js';

import ColorSwatch, { Swatch } from './Colors';

const { colors } = useTheme();

const CategoryTitle = styled.h3`
  margin: 0 0 6px;
`;

const SubcategoryName = styled.h4`
  margin: 0 0 6px;
`;

const Swatches = styled.div`
  display: flex;
  margin: 0 0 15px;
  flex-wrap: wrap;
`;

const StyledColorSwatch = styled(ColorSwatch)`
  flex-basis: ${(props) => props.categoryName === 'global' ? '100px' : '1px'};
  max-width: ${(props) => props.categoryName === 'global' ? '200px' : 'auto'};

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

const CategoryWrap = (categoryName, categoryColors) => (
  <>
    <Swatches>
      {getValues(categoryColors, (name) => {
        if (typeof categoryColors[name] === 'string' && chroma.valid(categoryColors[name])) {
          const copyTextName = categoryName === 'gray' ? `${categoryName}[${name}]` : `${categoryName}.${name}`;

          return (
            <StyledColorSwatch name={name}
                               section={categoryName}
                               color={categoryColors[name]}
                               copyText={`theme.colors.${copyTextName}`}
                               key={`${categoryName}-${name}`} />
          )
        }
      })}
    </Swatches>

    <div>
      {getValues(categoryColors, (name) =>
        typeof categoryColors[name] === 'object' && (
          <div key={`${categoryName}-${name}`}>
            <SubcategoryName>{categoryName} &mdash; {name}</SubcategoryName>

            <Swatches>
              {getValues(categoryColors[name], (subname) => (
                <StyledColorSwatch name={subname}
                                   categoryName={categoryName}
                                   color={categoryColors[name][subname]}
                                   copyText={`theme.colors.${categoryName}.${name}.${subname}`}
                                   key={`${categoryName}-${name}-${categoryColors[name][subname]}`} />
              ))}
            </Swatches>
          </div>
        )
      )}
    </div>
  </>
);

const Colors = () => (
  <>
    {getValues(colors, (categoryName) => (
      <div key={categoryName}>
          <CategoryTitle key={`title-${categoryName}`}>{categoryName}</CategoryTitle>
          {CategoryWrap(categoryName, colors[categoryName])}
      </div>
    ))}
  </>
);

<Colors />
```
