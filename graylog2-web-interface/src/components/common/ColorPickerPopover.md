```js
import { Button } from 'components/bootstrap';

class ColorPickerOverlayExample extends React.Component {
  constructor(props) {
    this.state = {
      color: undefined,
    };
    this.handleColorChange = this.handleColorChange.bind(this);
  }

  handleColorChange(color, _, hidePopover) {
    hidePopover();
    this.setState({ color: color });
  };

  render() {
    const { color } = this.state;

    return (
      <div>
        <p>{color ? `You picked ${color}.` : 'Pick a color'}</p>
        <ColorPickerPopover id="example-color-picker"
                            placement="right"
                            color={color}
                            triggerNode={<Button bsStyle="primary">Toggle color picker</Button>}
                            onChange={this.handleColorChange} />
      </div>
    );
  };
}

<ColorPickerOverlayExample />
```
