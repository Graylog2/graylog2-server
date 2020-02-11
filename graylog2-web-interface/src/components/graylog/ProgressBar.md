```js
<ProgressBar bars={60} />
```

```js
const value = 60;
<ProgressBar bars={[{ value, label: `${value}%` }]} />
```

```js
<div>
  <ProgressBar bars={[{ value: 40, bsStyle: 'success' }]} />
  <ProgressBar bars={[{ value: 20, bsStyle: 'info' }]} />
  <ProgressBar bars={[{ value: 60, bsStyle: 'warning' }]} />
  <ProgressBar bars={[{ value: 80, bsStyle: 'danger' }]} />
</div>
```

```js
<div>
  <ProgressBar bars={[{ value: 40, bsStyle: 'success', striped: true }]} />
  <ProgressBar bars={[{ value: 20, bsStyle: 'info', striped: true }]} />
  <ProgressBar bars={[{ value: 60, bsStyle: 'warning', striped: true }]} />
  <ProgressBar bars={[{ value: 80, bsStyle: 'danger', striped: true }]} />
</div>
```

```js
  <ProgressBar bars={[{ value: 45, bsStyle: 'danger', animated: true }]} />
```

```js
<ProgressBar bars={[
  { value: 35, bsStyle: 'success', striped: true },
  { value: 20, bsStyle: 'warning' },
  { value: 10, bsStyle: 'danger', animated: true }
]} />
```
