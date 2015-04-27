/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Displays temperature readings from a peripheral's advertisement
 */

var app = {
    initialize: function() {
        this.bindEvents();
        document.querySelector("#console").innerHTML = "-&deg;C";
        document.querySelector("#console").className = "0";
    },
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
        document.addEventListener('resume', this.onDeviceReady, false);
        document.addEventListener('pause', this.onPause, false);
    },
    onDeviceReady: function() {
        deviceId = window.gateway.getDeviceId();                                                                // get device ID from Gateway
        document.querySelector("#title").innerHTML = window.gateway.getDeviceName() + " Temperature";           // get device name from Gateway & set title
        ble.isEnabled(app.onEnable,function(){ document.querySelector("#console").innerHTML = "BLE OFF"; });    // if BLE enabled, goto: onEnable
    },
    onEnable: function() {
        ble.stopScan();
        ble.startScan([], app.onDiscover, app.onDeviceReady);                                                   // start BLE scan; if device discovered, goto: onDiscover
    },
    onDiscover: function(device) {
        if (device.id == deviceId) app.display(device);                                                         // if discovered device matches, goto: display
    },
    onPause: function() {
        ble.stopScan();                                                                                         // if user leaves app, stop BLE
    },
    display: function(device) {
        value    = app.getServiceData(device.advertising)[16]                                                   // get temperature value from the advertisement
        current  = document.querySelector("#console").className;                                                // rest of function is just for fancy display effects
        delta    = (current-value)/100;
        i        = 0;
        dig = function () {
            current  -= delta;
            document.querySelector("#console").className = Math.round(current);
            document.querySelector("#console").innerHTML = Math.round(current) + "&deg;C";
            if(i++<100) setTimeout(function () {dig()},10);
        };
        if (isNaN(parseInt(current))) {
            document.querySelector("#console").innerHTML = value + "&deg;C";
            document.querySelector("#console").className = value;
        } else if (current != value) dig();
        document.querySelector('body').style.background = 'rgb(' + Math.round(9.75 * value) + ',' + 96 + ',' + Math.round(255 - (9.75*value)) + ')';
    },
    getServiceData:function(device) {                                                                           // searches BLE advertisement for ServiceData
        scanRecord = new Uint8Array(device);
        index = 0;
        while (index < scanRecord.length) {
            length = scanRecord[index++];
            if (length == 0) return new Uint8Array(20);                                                         // returns nothing if there is no ServiceData
            type = scanRecord[index];
            if (type == 0) return new Uint8Array(20);                                                           // return nothing if advertisement is invalid
            data = scanRecord.subarray(index + 1, index + length); 
            if (type==22 && data[0]>=' ') return data;                                                          // returns ServiceData
            index += length; //advance
        }
    }
};

app.initialize();                                                                                               // start the app