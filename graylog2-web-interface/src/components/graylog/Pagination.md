```js
import { Pager } from 'components/graylog';

<Pager>
  <Pager.Item href="#">Previous</Pager.Item>{' '}
  <Pager.Item href="#">Next</Pager.Item>
</Pager>
```

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

```js
const ExamplePagination = ({}) => {
  const [active, setActive] = React.useState(7);
  let items = [];
  for (let number = 1; number <= 10; number++) {
    items.push(
      <Pagination.Item active={number === active}
                       key={`pagination-item-${number}`}
                       onClick={(item) => setActive(Number(item.target.innerText))}>
        {number}
      </Pagination.Item>
    );
  }

  return (
    <div>
      <Pagination bsSize="large">
        {items}
      </Pagination>
      <br />

      <Pagination bsSize="medium">
        {items}
      </Pagination>
      <br />

      <Pagination bsSize="small">
        {items}
      </Pagination>
    </div>
  )
};

<ExamplePagination />
```

```js
<Pagination>
  <Pagination.First />
  <Pagination.Prev />
  <Pagination.Item>{1}</Pagination.Item>
  <Pagination.Ellipsis />

  <Pagination.Item>{10}</Pagination.Item>
  <Pagination.Item>{11}</Pagination.Item>
  <Pagination.Item active>{12}</Pagination.Item>
  <Pagination.Item>{13}</Pagination.Item>
  <Pagination.Item disabled>{14}</Pagination.Item>

  <Pagination.Ellipsis />
  <Pagination.Item>{20}</Pagination.Item>
  <Pagination.Next />
  <Pagination.Last />
</Pagination>
```

```js
<div>
  <div>
    <Pagination bsSize="small"
                items={12}
                maxButtons={10}
                activePage={1}
                prev
                next
                first
                last />
  </div>
  <div>
    <Pagination items={12}
                maxButtons={10}
                activePage={12}
                prev
                next
                first
                last />
  </div>
  <div>
    <Pagination bsSize="large"
                items={12}
                maxButtons={10}
                activePage={7}
                prev
                next
                first
                last />
  </div>
</div>
```
