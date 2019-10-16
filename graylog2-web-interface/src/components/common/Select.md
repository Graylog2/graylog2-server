Simple option input:
```js
import createReactClass from 'create-react-class';

const SimpleSelect = createReactClass({
  getInitialState() {
    return {
      options: [
        { label: 'Option 1', value: 'one' },
        { label: 'Option 2', value: 'two' },
      ],
      selectedOption: '',
    };
  },

  onChange(nextValue) {
    this.setState({selectedOption: nextValue});
  },

  render() {
    const { options, selectedOption } = this.state;
    return (
      <div>
          <span>Selected option: "{selectedOption}"</span>
          <Select value={selectedOption}
                  onChange={this.onChange}
                  options={options}/>
      </div>
    );
  },
});

<SimpleSelect />
```

Multi select input:
```js
import createReactClass from 'create-react-class';
import { MultiSelect } from 'components/common';

const SimpleSelect = createReactClass({
  getInitialState() {
    return {
      options: [
        { spanish: 'uno', english: 'one' },
        { spanish: 'dos', english: 'two' },
        { spanish: 'tres', english: 'three' },
        { spanish: 'cuatro', english: 'four' },
        { spanish: 'cinco', english: 'five' },
      ],
      selectedOptions: 'one;three;five',
    };
  },

  onChange(nextValue) {
    this.setState({selectedOptions: nextValue});
  },

  render() {
    const { options, selectedOptions } = this.state;
    return (
      <div>
          <span>Selected options: "{selectedOptions}"</span>
          <MultiSelect value={selectedOptions}
                       onChange={this.onChange}
                       options={options}
                       displayKey="spanish"
                       valueKey="english"
                       delimiter=";"
                       allowCreate />
      </div>
    );
  },
});

<SimpleSelect />
```