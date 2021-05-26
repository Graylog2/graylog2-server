All spacing options are available via the ThemeProvider `theme.spacings` prop

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme } from 'styled-components';

const { spacings } = useTheme();

const Colors = () => (
  <>
    {Object.keys(spacings).map((space) => space !== 'px' && (
      <div key={space} style={{display:'flex',alignItems: 'center', marginBottom: 12}}>
        <div key={space} style={{height: spacings[space], width:spacings[space], backgroundColor: '#eee'}} />
        <span style={{paddingLeft: 12}}>spacings.{space}  ({spacings[space]} / {spacings.px[space] || space}px)</span>
      </div>
    ))}
  </>
);

<Colors />
```
