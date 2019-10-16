```js
import createReactClass from 'create-react-class';

const TimezoneSelectExample = createReactClass({
  getInitialState() {
    return {
      tz: 'Africa/Timbuktu',
    };
  },

  onChange(nextTZ) {
    this.setState({ tz: nextTZ });
  },

  render() {
    const { tz } = this.state;
    return (
      <div>
        <p>Selected time zone: {tz}</p>
        <TimezoneSelect value={tz} onChange={this.onChange} />
      </div>
    );
  },
});

<TimezoneSelectExample />
```
