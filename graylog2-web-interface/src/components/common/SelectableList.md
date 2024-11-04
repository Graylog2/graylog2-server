```js
class SelectableListExample extends React.Component {
  constructor() {
    this.state = {
      options: [
        { label: 'Uno', value: 'one' },
        { label: 'Dos', value: 'two' },
        { label: 'Tres', value: 'three' },
        { label: 'Cuatro', value: 'four' },
        { label: 'Cinco', value: 'five' },
      ],
      selectedOptions: [
        { label: 'Cuatro', value: 'four' },
        { label: 'Cuatro', value: 'four' },
      ],
    };
    this.onChange = this.onChange.bind(this);
  }

  onChange(nextSelectedOptions) {
    this.setState({ selectedOptions: nextSelectedOptions });
  };

  render() {
    const { options, selectedOptions } = this.state;
    return (
      <SelectableList selectedOptionsType="object"
                      options={options}
                      onChange={this.onChange}
                      selectedOptions={selectedOptions} />
    );
  }
}

<SelectableListExample />
```
