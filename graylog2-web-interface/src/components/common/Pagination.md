### Pagination
```js
const [currentPage, setCurrentPage] = React.useState(1);
<div>
    <Pagination totalPages={23}
                currentPage={currentPage}
                onChange={(nextPage) => { setCurrentPage(nextPage); }} />
</div>
```
