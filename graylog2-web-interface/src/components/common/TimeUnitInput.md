```js
class TimeUnitInputExample extends React.Component {
  constructor() {
    this.state = {
      value: undefined,
      unit: 'MINUTES',
      enabled: false,
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(value, unit, checked) {
    this.setState({ value: value, unit: unit, enabled: checked });
  };

  render() {
    const { value, unit, enabled } = this.state;
    return (
      <div>
        <p>{enabled ? `${value} ${unit}` : 'Disabled'}</p>
        <TimeUnitInput value={value}
                       unit={unit}
                       units={['SECONDS', 'MINUTES', 'DAYS']}
                       enabled={enabled}
                       update={this.onChange}
                       defaultValue={7} />
      </div>
    );
  }
}

<TimeUnitInputExample />
```
