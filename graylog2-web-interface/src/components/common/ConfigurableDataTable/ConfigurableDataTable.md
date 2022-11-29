Define custom cell and header renderer:
```js
import ConfigurableDataTable from './ConfigurableDataTable';

<ConfigurableDataTable attributes={['title', 'description']}
                       rows={[{
                         id: 'row-id',
                         title: 'Row title',
                         description: 'Row description',
                       }]}
                       availableAttributes={[
                         { id: 'title', title: 'Title' },
                         { id: 'description', title: 'Description' },
                       ]}
                       customCells={{
                         title: {
                           renderCell: (listItem) => `The title: ${listItem.title}`,
                         },
                       }}
                       customHeaders={{
                         title: {
                           renderHeader: (attribute) => `Custom ${attribute.title}`,
                         },
                       }}
/>
```

Render row actions:
```js
import ConfigurableDataTable from './ConfigurableDataTable';

<ConfigurableDataTable attributes={['title', 'description']}
                       rows={[{
                         id: 'row-id',
                         title: 'Row title',
                         description: 'Row description',
                       }]}
                       availableAttributes={[
                         { id: 'title', title: 'Title' },
                         { id: 'description', title: 'Description' },
                       ]}
                       rowActions={() => <div><button type="button">Actions</button></div>}/>
```


Only render a column when the user has the required permissions:
```js
import ConfigurableDataTable from './ConfigurableDataTable';

<ConfigurableDataTable attributes={['title', 'description']}
                       rows={[{
                         id: 'row-id',
                         title: 'Row title',
                         description: 'Row description',
                       }]}
                       availableAttributes={[
                         { id: 'title', title: 'Title' },
                         { id: 'description', title: 'Description' },
                       ]}
                       attributePermissions={{
                         description: {
                           permissions: ['description:read'],
                         },
                       }} />
```