Simple option input:
```js
class SimpleSelect extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      options: [
        { label: 'Option 1', value: 'one' },
        { label: 'Option 2', value: 'two' },
      ],
      selectedOption: '',
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(nextValue) {
    this.setState({selectedOption: nextValue});
  };

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
  }
}

<SimpleSelect />
```

Multi select input:
```js
import { MultiSelect } from 'components/common';

class SimpleSelect extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      options: [
        { spanish: 'uno', english: 'one' },
        { spanish: 'dos', english: 'two' },
        { spanish: 'tres', english: 'three' },
        { spanish: 'cuatro', english: 'four' },
        { spanish: 'cinco', english: 'five' },
      ],
      selectedOptions: 'one;three;five',
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(nextValue) {
    this.setState({selectedOptions: nextValue});
  };

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
  }
}

<SimpleSelect />
```