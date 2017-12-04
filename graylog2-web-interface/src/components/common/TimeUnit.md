Display values in minutes and seconds:
```js
<div>
  <TimeUnit value={4} unit="MINUTES" />
  <br />
  <TimeUnit value={7} unit="SECONDS" />
</div>
```

Use zero as never:
```js
<TimeUnit value={0} unit="MINUTES" zeroIsNever />
```
