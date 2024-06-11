Helpful to animate progress for a specific duration in a loop.

```tsx
import { useState, useEffect } from 'react';
import { Button } from 'components/bootstrap';
import ProgressAnimation from './ProgressAnimation';
import styled from 'styled-components';

const Example = () => {
  const [animationStartCount, setAnimationStartCount] = useState(1);

  useEffect(() => {
    const interval = setInterval(() => {
      setAnimationStartCount((cur) => cur + 1);
    }, 10000)
      
    return () => {
      clearInterval(interval);
    } 
  }, [animationStartCount]);
  
  return (
    <Button>
      A button
      {!!animationStartCount && <ProgressAnimation $animationDuration={10000} key={`animation-${animationStartCount}`} $increase={false} />}
    </Button>
  )
}

<Example />
```
