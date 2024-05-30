Helpful to animate progress for a specific duration in a loop.

```tsx
import { useState, useEffect } from 'react';
import { Button } from 'components/bootstrap';
import ProgressAnimation from './ProgressAnimation';
import styled from 'styled-components';

const Label = styled.span`
  z-index: 1;
`

const Example = () => {
  const [animationStart, setAnimationStartCount] = useState(1);

  useEffect(() => {
    const interval = setInterval(() => {
      setAnimationStartCount((cur) => cur + 1);
    }, 10000)
      
    return () => {
      clearInterval(interval);
    } 
  }, []);
  return (
    <Button onClick={() => setAnimationStartCount((cur) => cur + 1)}>
      {!!animationStart && <ProgressAnimation $animationDuration={10000} key={`animation-${animationStart}`} />}
      <Label>Click to restart animation</Label>
    </Button>
  )
}

<Example />
```
