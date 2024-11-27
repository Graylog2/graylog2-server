```js
import SortableList from './SortableList'

class SortableListExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      list: [
        { id: 'one', title: 'One' },
        { id: 'two', title: 'Two' },
        { id: 'three', title: 'Three' },
        { id: 'four', title: 'Four' },
        { id: 'five', title: 'Five' },
      ],
      sortedList: [],
    };
    this.onSortList = this.onSortList.bind(this);
  }

  onSortList(nextSortedList) {
    this.setState({ sortedList: nextSortedList });
  };

  render() {
    const { list, sortedList } = this.state;

    return (
      <div>
        <p>Sorted list: {sortedList.map(item => item.title).join(', ')}</p>
        <SortableList items={list} onMoveItem={this.onSortList} />
      </div>
    );
  }
}

<SortableListExample />
```
