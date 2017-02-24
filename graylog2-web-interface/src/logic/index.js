const req = require.context('./', true, /.[jt]s(x)?$/);
req.keys().forEach(req);

