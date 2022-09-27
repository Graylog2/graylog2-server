Font settings are accesible from within `theme.fonts` object.

### Family - _`theme.fonts.family`_

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';

import ColorSwatch, { Swatch } from './Colors';

const { fonts } = useTheme();

const FontFamilies = () => (
  <ul>
    {Object.entries(fonts.family).map(([fontId, fontNames]) => {
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

const FontSizes = () => (
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

<FontSizes />
```

### Line height - _`theme.fonts.lineHeight`_

The default line height works well for all text, no matter its size. Make sure to always rely on the default line height
and do not define a custom line height for text. This way we can unify the font style across the application.

Sometimes you may want to define a line height for an element like an icon to adjust its height.
In case the element height should not be relative to the text size, make sure to define the line height in `px` and not in `rem` or without a unit.

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';

import ColorSwatch, { Swatch } from './Colors';

const { fonts } = useTheme();

const FontLineHeights = () => (
  <ul>
    {Object.entries(fonts.lineHeight).map(([lineHeightId, lineHeightValue]) => {
      return <li><b>{lineHeightId}</b>: {lineHeightValue}</li>
    })}
  </ul>
);

<FontLineHeights />
```