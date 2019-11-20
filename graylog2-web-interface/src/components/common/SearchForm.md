Search form with uncontrolled query string:
```js
import createReactClass from 'create-react-class';
import { Button } from 'components/graylog';

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

Search form with controlled query string and help:

```js
import createReactClass from 'create-react-class';
import { Button } from 'components/graylog';
import { Icon } from 'components/common';

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
                    queryWidth={300}
                    queryHelpComponent={(
                      <Button onClick={() => alert('help!')} bsStyle="link">
                        <Icon name="question-circle" />
                      </Button>)}
                    useLoadingState />
      </div>
    );
  },
});

<SearchFormExample />
```
