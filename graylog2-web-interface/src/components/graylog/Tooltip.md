### Basic

```js
const styles = { position: 'relative', display: 'inline-block' };

<div>
  <Tooltip placement="right" className="in" id="tooltip-right" style={styles}>
    Tooltip right
  </Tooltip>

  <Tooltip placement="top" className="in" id="tooltip-top" style={styles}>
    Tooltip top
  </Tooltip>

  <Tooltip placement="left" className="in" id="tooltip-left" style={styles}>
    Tooltip left
  </Tooltip>

  <Tooltip placement="bottom" className="in" id="tooltip-bottom" style={styles}>
    Tooltip bottom
  </Tooltip>
</div>
```

### Hover

```js
import { ButtonToolbar, Button, OverlayTrigger } from './index';

const TooltipExample = () => {
  const tooltip = (
    <Tooltip id="tooltip">
      <strong>Yo!</strong> Check this info.
    </Tooltip>
  );

  return (
    <ButtonToolbar>
      <OverlayTrigger placement="left" overlay={tooltip}>
        <Button bsStyle="default">Hover Example Left!</Button>
      </OverlayTrigger>

      <OverlayTrigger placement="top" overlay={tooltip}>
        <Button bsStyle="default">Hover Example Top!</Button>
      </OverlayTrigger>

      <OverlayTrigger placement="bottom" overlay={tooltip}>
        <Button bsStyle="default">Hover Example Bottom!</Button>
      </OverlayTrigger>

      <OverlayTrigger placement="right" overlay={tooltip}>
        <Button bsStyle="default">Hover Example Right!</Button>
      </OverlayTrigger>
    </ButtonToolbar>
  )
};

<TooltipExample />
```
