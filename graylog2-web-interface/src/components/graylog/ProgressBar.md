```js
<ProgressBar now={60} />
```

```js
const now = 60;
<ProgressBar now={now} label={`${now}%`} />
```

```js
<div>
  <ProgressBar bsStyle="success" now={40} />
  <ProgressBar bsStyle="info" now={20} />
  <ProgressBar bsStyle="warning" now={60} />
  <ProgressBar bsStyle="danger" now={80} />
</div>
```

```js
<div>
  <ProgressBar striped bsStyle="success" now={40} />
  <ProgressBar striped bsStyle="info" now={20} />
  <ProgressBar striped bsStyle="warning" now={60} />
  <ProgressBar striped bsStyle="danger" now={80} />
</div>
```

```js
<ProgressBar active now={45} />
```

```js
<ProgressBar>
  <ProgressBar striped bsStyle="success" now={35} key={1} />
  <ProgressBar bsStyle="warning" now={20} key={2} />
  <ProgressBar active bsStyle="danger" now={10} key={3} />
</ProgressBar>
```
