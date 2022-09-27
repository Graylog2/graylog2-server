Font settings are accesible from within `theme.fonts` object.

### Family - _`theme.fonts.family`_

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';

import ColorSwatch, { Swatch } from './Colors';

const { fonts } = useTheme();

const FontFamilies = () => (
  <ul>
    {Object.entries(fonts.size).map(([fontId, fontNames]) => {
      return <li><b>{fontId}</b>: {fontNames}</li>
    })}
  </ul>
);

<FontFamilies />
```

### Size - _`theme.fonts.size`_

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';

import ColorSwatch, { Swatch } from './Colors';
import { ROOT_FONT_SIZE } from 'theme/constants';

const { fonts } = useTheme();

const FontFamilies = () => (
  <ul>
    {Object.entries(fonts.size).map(([sizeId, sizeValue]) => {
      return (
        <li>
          <b>{sizeId}</b>: {sizeValue}
          {sizeId !== 'root' && <i> ({sizeValue.replace(/rem|em/i, '') * ROOT_FONT_SIZE}px)</i>}
        </li>
      )
    })}
  </ul>
);

<FontFamilies />
```