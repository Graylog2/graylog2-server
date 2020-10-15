```js
const entities = [
    {
      id: '1',
      title: 'Dr. Emmett Brown',
      nickname: 'Doc',
      roles: ['Doctor', 'Human'],
      description: '1.21 GIGAWATTS!!!',
    },
    {
      id: '2',
      title: 'Marty McFly',
      nickname: 'Marty',
      roles: ['Student', 'Human'],
      description: 'Nobody calls me chicken',
    },
];
const filterKeys = ['title', 'nickname'];
const headers = ['', 'Name', 'Nickname', 'Role', 'Quote'];

const rowFormatter = (entity, idx) => {
  return (
    <tr key={entity.id}>
      <td>{idx + 1}</td>
      <td>{entity.title}</td>
      <td>{entity.nickname}</td>
      <td>{entity.roles.join(', ')}</td>
      <td>{entity.description}</td>
    </tr>
  );
};

<DataTable id="entity-list"
           className="table-hover"
           headers={headers}
           headerCellFormatter={(header) => <th>{header}</th>}
           sortByKey={'title'}
           rows={entities}
           filterBy="role"
           filterSuggestions={['Doctor', 'Student', 'Human']}
           dataRowFormatter={rowFormatter}
           filterLabel="Filter entities"
           filterKeys={filterKeys} />
```

`DataTable` with `sortBy` function:

```js
const entities = [
    {
      id: '1',
      title: 'Dr. Emmett Brown',
      nickname: 'Doc',
      roles: ['Doctor', 'Human'],
      description: '1.21 GIGAWATTS!!!',
    },
    {
      id: '2',
      title: 'Marty McFly',
      nickname: 'Marty',
      roles: ['A Student', 'Human'],
      description: 'Nobody calls me chicken',
    },
];
const headers = ['', 'Name', 'Nickname', 'Role', 'Quote'];

const rowFormatter = (entity, idx) => {
  return (
    <tr key={entity.id}>
      <td>{idx + 1}</td>
      <td>{entity.title}</td>
      <td>{entity.nickname}</td>
      <td>{entity.roles.join(', ')}</td>
      <td>{entity.description}</td>
    </tr>
  );
};

<DataTable id="entity-list"
           className="table-hover"
           headers={headers}
           headerCellFormatter={(header) => <th>{header}</th>}
           sortBy={entity => entity.roles[0]}
           rows={entities}
           filterBy="role"
           filterSuggestions={['Doctor', 'Student', 'Human']}
           dataRowFormatter={rowFormatter}
           filterKeys={[]} />
```
