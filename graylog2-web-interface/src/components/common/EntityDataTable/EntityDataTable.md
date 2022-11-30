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


Change the width of a column, with the related column renderer. Column renderers can have
- either a `width` defined as a fraction like `2`. If no width is defined we are using a default of `1`.
  The width defines which fraction of the assignable space the column should claim. It works similar to the css attribute `flex`.
  Optionally you can also define a `minWidth` to override the default min width for flexible columns. This can be helpful to ensure a column has enough space, no matter how large the table width is.
- or a `staticWidth` in px. Useful when the cells contain e.g. just an icon and its width never changes.

Please have a look at the default column renderers defined in the `EntityDataTable` since they contain predefined widths for common attributes like `description`.

```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable visibleColumns={['title', 'summary', 'status']}
                 data={[{
                   id: 'row-id',
                   title: 'Entity title',
                   summary: 'Entity summary',
                   status: 'status',
                 }]}
                 columnDefinitions={[
                   { id: 'title', title: 'Title' },
                   { id: 'summary', title: 'Summary' },
                   { id: 'status', title: 'Status' },
                 ]}
                 columnRenderers={{
                   summary: {
                     width: 2,
                     minWidth: 200
                   },
                   status: {
                     staticWidth: 100
                   }
                 }}/>
```
