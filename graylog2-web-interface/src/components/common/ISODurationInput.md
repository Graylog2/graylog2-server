```js
let duration = 'PT1M';

<ISODurationInput id="iso-duration-input"
                duration={duration}
                update={(nextDuration) => { console.log(`Duration set to ${nextDuration}`) }}
                label="ISO duration input"
                help="Type an ISO duration."
                validator={(milliseconds) => milliseconds >= 1 }
                errorText="Invalid duration!"/>
```