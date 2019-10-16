```js
import createReactClass from 'create-react-class';

const ColorPickerExample = createReactClass({
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
        <ColorPicker color={color} onChange={this.handleColorChange} />
      </div>
    );
  },
});

<ColorPickerExample />
```