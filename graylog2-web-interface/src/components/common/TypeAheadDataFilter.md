```js
const lodash = require('lodash');

class TypeAheadDataFilterExample extends React.Component {
  constructor() {
    const list = [
      { id: '1', title: 'One', description: 'First number', tags: ['one', 'number'] },
      { id: '2', title: 'Two', description: 'Second number', tags: ['two', 'number'] },
      { id: '3', title: 'Three', description: 'Third number', tags: ['three', 'number'] },
      { id: '4', title: 'Four', description: 'Fourth number', tags: ['four', 'number'] },
      { id: '5', title: 'A', description: 'First letter', tags: ['a', 'letter'] },
    ];
    this.state = {
      list: list,
      filteredList: list,
    };
    this.onDataFiltered = this.onDataFiltered.bind(this);
  }

  onDataFiltered(filteredList) {
    this.setState({ filteredList: filteredList });
  };

  render() {
    const { list, filteredList } = this.state;
    const suggestions = lodash.uniq(lodash.flatten(list.map((item) => item.tags)));

    return (
      <div>
        <TypeAheadDataFilter id="awesome-filter"
                             label="Filter list"
                             data={list}
                             displayKey="title"
                             filterBy="tag"
                             filterSuggestions={suggestions}
                             searchInKeys={['title', 'description']}
                             onDataFiltered={this.onDataFiltered} />
        <div>
          <dl>
            {filteredList.map((item) => [
              <dt key={`${item.id}-title`}>{item.title}</dt>,
              <dd key={`${item.id}-desc`}>{item.description}</dd>,
            ])}
          </dl>
        </div>
      </div>
    );
  }
}

<TypeAheadDataFilterExample />
```
