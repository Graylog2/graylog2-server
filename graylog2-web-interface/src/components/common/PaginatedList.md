```js
import createReactClass from 'create-react-class';

const PaginatedListExample = createReactClass({
  getInitialState() {
    const items = [];
    for(let i = 1; i <= 12; i++)
      items.push(i);

    return {
      currentPage: 0,
      items: items,
      pageSize: 5,
    };
  },

  onPageChange(currentPage, pageSize) {
    this.setState({
      currentPage: currentPage - 1,
      pageSize: pageSize,
    });
  },

  render() {
    const { currentPage, items, pageSize } = this.state;

    const paginatedItems = items.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

    return (
      <PaginatedList totalItems={items.length}
                     pageSize={pageSize}
                     onChange={this.onPageChange}
                     pageSizes={[5, 10, 20]}>
        <table className="table">
          <thead>
            <tr>
              <th>Item</th>
            </tr>
          </thead>
          <tbody>
            {paginatedItems.map((item) => <tr key={item}><td>{item}</td></tr>)}
          </tbody>
        </table>
      </PaginatedList>
    );
  },
});

<PaginatedListExample />
```
