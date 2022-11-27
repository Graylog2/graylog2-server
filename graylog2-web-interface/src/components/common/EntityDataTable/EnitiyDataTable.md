Define custom cell and header renderer:
```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable visibleColumns={['title', 'description']}
                 data={[{
                   id: 'row-id',
                   title: 'Row title',
                   description: 'Row description',
                 }]}
                 columnDefinitions={[
                   { id: 'title', title: 'Title' },
                   { id: 'description', title: 'Description' },
                 ]}
                 columnRenderers={{
                   title: {
                     renderCell: (listItem) => `The title: ${listItem.title}`,
                     renderHeader: (attribute) => `Custom ${attribute.title}`,
                   },
                 }}
/>
```

Render row actions:
```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable visibleColumns={['title', 'description']}
                 data={[{
                   id: 'row-id',
                   title: 'Row title',
                   description: 'Row description',
                 }]}
                 columnDefinitions={[
                   { id: 'title', title: 'Title' },
                   { id: 'description', title: 'Description' },
                 ]}
                 rowActions={() => <div><button type="button">Actions</button></div>}/>
```


Only render a column when the user has the required permissions:
```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable visibleColumns={['title', 'description']}
                 data={[{
                   id: 'row-id',
                   title: 'Row title',
                   description: 'Row description',
                 }]}
                 columnDefinitions={[
                   { id: 'title', title: 'Title' },
                   { id: 'description', title: 'Description' },
                 ]}
                 attributePermissions={{
                   description: {
                     permissions: ['description:read'],
                   },
                 }} />
```