iot-gateway
===========

Our implementation of the gateway described in "[The Internet of Things Has a Gateway Problem](http://dl.acm.org/citation.cfm?id=2699344)". 

- `/app/Gateway` : the Android Gateway app implementing BLE Profile Proxy.
- `/app/Peripheral` : an Android Peripheral app to advertises & tests API params for BLE peripherals.
- `/datacloud` : a simple responsive server implemention using NodeJS, demonstrating API for servers.
- `/squall` : [squall](https://github.com/helena-project/squall) apps that use Gateway API.
- [`index.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/index.html) : log of received data at the gateway, data cloud, and incentive cloud
- [`example.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/example.html) : example visualization of data from the Tessel app
- [`raw.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/raw.html) : raw output of data sent to data cloud (when received at GATD)
