```js
class ColorPickerExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      color: undefined,
    };
    this.handleColorChange = this.handleColorChange.bind(this);
  };

  handleColorChange(color) {
    this.setState({ color: color });
  };

  render() {
    const { color } = this.state;
    return (
      <div>
        <p>{color ? `You picked ${color}.` : 'Pick a color'}</p>
        <ColorPicker color={color} onChange={this.handleColorChange} />
      </div>
    );
  };
}

<ColorPickerExample />
```