```js
<div style={{ height: 200, position: 'relative' }}>
  <Popover
    id="popover-basic"
    placement="right"
    positionLeft={200}
    positionTop={50}
    title="Popover right"
  >
    And here's some <strong>amazing</strong> content. It's very engaging. right?
  </Popover>
</div>
```

```js
import { ButtonToolbar, OverlayTrigger, Button, Popover } from 'components/graylog';

const popoverLeft = (
  <Popover id="popover-positioned-left" title="Popover left">
    <strong>Holy guacamole!</strong> Check this info.
  </Popover>
);

const popoverTop = (
  <Popover id="popover-positioned-top" title="Popover top">
    <strong>Holy guacamole!</strong> Check this info.
  </Popover>
);

const popoverBottom = (
  <Popover id="popover-positioned-bottom" title="Popover bottom">
    <strong>Holy guacamole!</strong> Check this info.
  </Popover>
);

const popoverRight = (
  <Popover id="popover-positioned-right" title="Popover right">
    <strong>Holy guacamole!</strong> Check this info.
  </Popover>
);

<ButtonToolbar>
  <OverlayTrigger trigger="click" placement="left" overlay={popoverLeft}>
    <Button>Holy guacamole!</Button>
  </OverlayTrigger>
  <OverlayTrigger trigger="click" placement="top" overlay={popoverTop}>
    <Button>Holy guacamole!</Button>
  </OverlayTrigger>
  <OverlayTrigger trigger="click" placement="bottom" overlay={popoverBottom}>
    <Button>Holy guacamole!</Button>
  </OverlayTrigger>
  <OverlayTrigger trigger="click" placement="right" overlay={popoverRight}>
    <Button>Holy guacamole!</Button>
  </OverlayTrigger>
</ButtonToolbar>
```
