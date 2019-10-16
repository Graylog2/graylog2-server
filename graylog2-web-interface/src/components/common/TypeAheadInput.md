```js
import createReactClass from 'create-react-class';

const TypeAheadInputExample = createReactClass({
  getInitialState() {
    return {
      selectedNumber: undefined,
    };
  },

  onChange(event, selectedNumber) {
    this.setState({ selectedNumber: selectedNumber.number });
  },

  render() {
    const { selectedNumber } = this.state;
    const suggestions = ['One', 'Two', 'Three', 'Four', 'Five'];

    return (
      <div>
        <p>Selected number: {selectedNumber === undefined ? 'none' : selectedNumber}</p>
        <TypeAheadInput id="typeahead-input"
                        label="Numbers"
                        displayKey="number"
                        suggestionText="Select number"
                        suggestions={suggestions}
                        onSuggestionSelected={this.onChange} />
      </div>
    );
  },
});

<TypeAheadInputExample />
```
