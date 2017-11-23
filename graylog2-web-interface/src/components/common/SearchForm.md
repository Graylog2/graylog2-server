```js
const createReactClass = require('create-react-class');
const Button = require('react-bootstrap').Button;

const SearchFormExample = createReactClass({
  getInitialState() {
    return {
      query: '',
    };
  },

  onSearch(query, resetState) {
    this.setState({ query: query });
    setTimeout(resetState, 2 * 1000);
  },

  onReset() {
    this.setState({ query: '' });
  },

  render() {
    return (
      <div>
        <span>Query: "{this.state.query}"</span>
        <SearchForm onSearch={this.onSearch}
                    onReset={this.onReset}
                    searchButtonLabel="Find"
                    resetButtonLabel="Nevermind"
                    searchBsStyle="info"
                    label="Search"
                    useLoadingState>
          <Button>Add-on</Button>
        </SearchForm>
      </div>
    );
  },
});

<SearchFormExample />
```
