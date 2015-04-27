iot-gateway
===========

Our implementation of the gateway described in "[The Internet of Things Has a Gateway Problem](http://dl.acm.org/citation.cfm?id=2699344)". 

- `/app/Gateway` : the Android Gateway app implementing BLE Profile Proxy.
- `/app/Peripheral` : an Android Peripheral app to advertise & test API for BLE peripherals.
- `/datacloud` : a simple responsive server implemention using NodeJS; demonstrates API for servers.
- `/squall` : [squall](https://github.com/helena-project/squall) apps that use Gateway API.
- [`index.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/index.html) : log of received data at the gateway, data cloud, and incentive cloud
- [`example.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/example.html) : example visualization of data from the Tessel app
- [`raw.html`](http://htmlpreview.github.io/?https://github.com/lab11/iot-gateway/blob/master/raw.html) : raw output of data sent to data cloud (when received at GATD)


API
---

To use Gateway, a peripheral specifies the following in the advertisement service data:
- Shortened Destination URL (up to 14 characters), sans protocol [14 bytes]
- Incentive Program Level (0-15) [.5 bytes]
- Reliability Level (0-15) [.5 bytes]
- Gateway Services Requested [1 byte]
  - Time Access (0/1)
  - GPS Access (0/1)
  - Accelerometer Access (0/1)
  - Ambient Light Sensor Access (0/1)
  - User Text Input Request (0/1)
  - User Camera Input Request (0/1)
  - Web UI Display Request (0/1)
  - IP Over BLE Request (Reserved) (0/1)
- Custom App Data (up to 10 bytes)

For instance, if a peripheral advertised `676f6f2e676c2f6a524d7845300077C0BEEF`, it would be parsed as:
- Destination: `676f6f2e676c2f6a524d784530000` -> `"goo.gl/jRMxE0"`
  - resolves to http://gatewaycloud.elasticbeanstalk.com, a data server expecting POSTs from Gateway
- Incentive Program Level: `7`
- Reliability: `7`
- Gateway Services Requested : `C0` -> Time Access & GPS Access
- Custom App Data: `BEEF`

