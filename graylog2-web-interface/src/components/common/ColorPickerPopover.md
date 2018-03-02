```js
const createReactClass = require('create-react-class');
const { Button } = require('react-bootstrap');

const ColorPickerOverlayExample = createReactClass({
  getInitialState() {
    return {
      color: undefined,
    };
  },

  handleColorChange(color) {
    this.setState({ color: color });
  },

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
  },
});

<ColorPickerOverlayExample />
```