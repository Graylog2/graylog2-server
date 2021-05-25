All spacing options are available via the ThemeProvider `theme.spacing` prop

```jsx noeditor
import React, { useEffect } from 'react';
import styled, { useTheme  } from 'styled-components';

const { spacing } = useTheme();

console.log({spacing})

const Colors = () => (
  <>
    {Object.keys(spacing).map((space) => space !== 'px' && (
      <div key={space} style={{display:'flex',alignItems: 'center', marginBottom: 12}}>
        <div key={space} style={{height: spacing[space], width:spacing[space], backgroundColor: '#eee'}} />
        <span style={{paddingLeft: 12}}>spacing.{space}  ({spacing.px[space] || space}px)</span>
      </div>
    ))}
  </>
);

<Colors />
```
