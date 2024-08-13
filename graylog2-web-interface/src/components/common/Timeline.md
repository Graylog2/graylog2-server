Simple timeline:

```tsx
import { Icon } from 'components/common';

const Example = () => (
  <Timeline bulletSize={24} active={1} color="success">
    <Timeline.Item title="Step 1" bullet={<Icon name="check" />}>
      A description of step 1
    </Timeline.Item>
    <Timeline.Item title="Step 2" bullet={<Icon name="check" />}>
      A description of step 2
    </Timeline.Item>
    <Timeline.Item title="Step 3">
      A description of step 3
    </Timeline.Item>
    <Timeline.Item title="Step 4">
      A description of step 4
    </Timeline.Item>
  </Timeline>
);

<Example />
```
