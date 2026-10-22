`WarningRow` is a `Row` with a warning-colored border. Use it to draw attention to a section that needs the user's
attention, for example a disabled feature.

```js
import { Col } from 'components/bootstrap';
import WarningRow from './WarningRow';

<WarningRow className="content">
  <Col md={12}>This feature is currently disabled.</Col>
</WarningRow>;
```
