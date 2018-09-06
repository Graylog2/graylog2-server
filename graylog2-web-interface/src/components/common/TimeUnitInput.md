```js
const createReactClass = require('create-react-class');

const TimeUnitInputExample = createReactClass({
  getInitialState() {
    return {
      value: undefined,
      unit: 'MINUTES',
      enabled: false,
    };
  },

  onChange(value, unit, checked) {
    this.setState({ value: value, unit: unit, enabled: checked });
  },

  render() {
    const { value, unit, enabled } = this.state;
    return (
      <div>
        <p>{enabled ? `${value} ${unit}` : 'Disabled'}</p>
        <TimeUnitInput value={value}
                       unit={unit}
                       enabled={enabled}
                       update={this.onChange}
                       defaultValue={7} />
      </div>
    );
  },
});

<TimeUnitInputExample />
```
