Search form with uncontrolled query string:
```js
import { Button } from 'components/bootstrap';

class SearchFormExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      query: '',
    };
    this.onSearch = this.onSearch.bind(this);
    this.onReset = this.onReset.bind(this);
  }

  onSearch(query, resetState) {
    this.setState({ query: query });
    setTimeout(resetState, 2 * 1000);
  };

  onReset() {
    this.setState({ query: '' });
  };

  render() {
    return (
      <div>
        <span>Query: "{this.state.query}"</span>
        <SearchForm onSearch={this.onSearch}
                    onReset={this.onReset}
                    label="Search"
                    useLoadingState>
          <Button>Add-on</Button>
        </SearchForm>
      </div>
    );
  }
}

<SearchFormExample />
```

Search form with controlled query string and help:

```js
import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';

class SearchFormExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      queryTemplate: 'test',
      query: '',
    };
    this.onQueryTemplateChange = this.onQueryTemplateChange.bind(this);
    this.onSearch = this.onSearch.bind(this);
    this.onReset = this.onReset.bind(this);
  }

  onQueryTemplateChange(e) {
    this.setState({ queryTemplate: e.target.value });
  };

  onSearch(query, resetState) {
    this.setState({ query: query });
    setTimeout(resetState, 2 * 1000);
  };

  onReset() {
    this.setState({ query: '' });
  };

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
                    label="Search"
                    queryHelpComponent={(
                      <Button onClick={() => alert('help!')} bsStyle="link">
                        <Icon name="help" />
                      </Button>)}
                    useLoadingState />
      </div>
    );
  }
}

<SearchFormExample />
```
