```js
const createReactClass = require('create-react-class');

const FilterInputExample = createReactClass({

  getInitialState() {
    return {
      filtered_rows: this.rows(),
    };
  },

  rows() {
    return [
      "Rand",
      "Mat",
      "Perrin",
      "Egwene",
      "Nynaeve",
    ]
  },

  onChange(filter) {
    if (filter === '') {
      this.setState({ filtered_rows: this.rows() });
      return;
    }
    const newRows = this.state.filtered_rows.filter((row) => {
      const regexp = RegExp(filter, 'i');
      return regexp.test(row);
    });
    this.setState({ filtered_rows: newRows });
  },

  render() {
    const rows = this.state.filtered_rows.map(e => <li key={e}>{e}</li>);
    return (
      <div>
        <FilterInput onChange={this.onChange} />
        <ul>
          {rows}
        </ul>
      </div>
    );
  },
});

<FilterInputExample />
```
