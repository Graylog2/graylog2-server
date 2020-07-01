### Default
```js
import { Pager } from 'components/graylog';

<Pager>
  <Pager.Item href="#">Previous</Pager.Item>{' '}
  <Pager.Item href="#">Next</Pager.Item>
</Pager>
```

### Aligned
```js
import { Pager } from 'components/graylog';

<Pager>
  <Pager.Item previous href="#">
    &larr; Previous Page
  </Pager.Item>
  <Pager.Item next href="#">
    Next Page &rarr;
  </Pager.Item>
</Pager>
```

### Disabled
```js
import { Pager } from 'components/graylog';

<Pager>
  <Pager.Item previous href="#">
    &larr; Previous
  </Pager.Item>
  <Pager.Item disabled next href="#">
    Next &rarr;
  </Pager.Item>
</Pager>
```

### Pagination
```js
<div>
    <Pagination totalPages={12}
                currentPage={7}
                onChange={(nextPage) => { alert(nextPage); }} />
</div>
```
