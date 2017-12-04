```js
<ClipboardButton title="Copy me!"
                 text="Copy me!" />
```

`ClipboardButton` with `onSuccess` callback:
```js
<div>
    <p id="clipboard-button-2">This text will be copied to your clipboard.</p>
    <ClipboardButton title="Copy me too!"
                     target="#clipboard-button-2"
                     onSuccess={() => alert('Copied to clipboard!')}
                     bsStyle="info"
                     bsSize="small" />
</div>
```