Search form with uncontrolled query string:
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

Search form with controlled query string:

```js
const createReactClass = require('create-react-class');
const Button = require('react-bootstrap').Button;

const SearchFormExample = createReactClass({
  getInitialState() {
    return {
      queryTemplate: 'test',
      query: '',
    };
  },

  onQueryTemplateChange(e) {
    this.setState({ queryTemplate: e.target.value });
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
        Query template:
        <input type="text" value={this.state.queryTemplate} onChange={this.onQueryTemplateChange} />
        <br />
        <span>Query: "{this.state.query}"</span>
        <SearchForm onSearch={this.onSearch}
                    onReset={this.onReset}
                    query={this.state.queryTemplate}
                    searchBsStyle="info"
                    label="Search"
                    useLoadingState />
      </div>
    );
  },
});

<SearchFormExample />
```
