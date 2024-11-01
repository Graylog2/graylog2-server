```js
class TimezoneSelectExample extends React.Component {
  constructor() {
    this.state = {
      tz: 'Africa/Timbuktu',
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(nextTZ) {
    this.setState({ tz: nextTZ });
  };

  render() {
    const { tz } = this.state;
    return (
      <div>
        <p>Selected time zone: {tz}</p>
        <TimezoneSelect value={tz} onChange={this.onChange} />
      </div>
    );
  }
}

<TimezoneSelectExample />
```
