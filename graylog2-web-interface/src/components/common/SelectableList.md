```js
import createReactClass from 'create-react-class';

const SelectableListExample = createReactClass({
  getInitialState() {
    return {
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
  },

  onChange(nextSelectedOptions) {
    this.setState({ selectedOptions: nextSelectedOptions });
  },

  render() {
    const { options, selectedOptions } = this.state;
    return (
      <SelectableList selectedOptionsType="object"
                      options={options}
                      onChange={this.onChange}
                      selectedOptions={selectedOptions} />
    );
  },
});

<SelectableListExample />
```
