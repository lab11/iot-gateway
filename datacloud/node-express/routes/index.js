var express = require('express');
var router  = express.Router();
var request = require('request');
// var io = require('socket.io')(require('http').Server(express));

var CCUUID      = "00002000-0000-1000-8000-00805f9b34fb";
var CCDATAUUID  = "00002b00-0000-1000-8000-00805f9b34fb";
var CCREADYUUID = "00002b04-0000-1000-8000-00805f9b34fb";

var hex2a = function(hexx) {
    var hex = hexx.toString();//force conversion
    var str = '';
    for (var i = 0, s; i < hex.length; i += 2) str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    return str;
}

/* GET home page. */
router.get('/', function(req, res, next) { res.render('index', { title: 'Gateway Cloud' }); });


/* POST response */
router.post('/', function(req, res) {
    console.log(JSON.stringify(req.body,null,4));


    /* OPO */

    if (typeof req.body.NAME != "undefined" && req.body.NAME == "Opo") {

        if (typeof req.body.ATTRIBUTES == "undefined") {
            res.send(JSON.stringify({
                "name" : req.body.NAME,
                "device_id" : req.body.DEVICE_ID,
                "services":[
                    {
                        "uuid": CCUUID,
                        "characteristics":[
                            {"uuid":CCDATAUUID,"action":"indicate","ccmanager_uuid":CCREADYUUID,"time":60000,"response":"accumulate"}
                       ]
                    } 
                ]
            }));
        } else {
            res.send("RE-RESPOND SUCCESSFUL! DISCONNECT BLE NOW!");
            data = req.body;
            data["OPO"] = req.body.ATTRIBUTES[0];
            request.post({
                url:'http://gatd.eecs.umich.edu:8081/SgYPCHTR5a',
                headers:{"content-type":"application/json"},
                body:JSON.stringify(data)},
                function(error,response,body){console.log(body);});
        }
    }


    /* TESSEL */

    else if (typeof req.body.DEVICE_ID != "undefined" && req.body.DEVICE_ID == "00:07:80:79:31:67")
        if (typeof req.body.ATTRIBUTES == "undefined") {
            res.send(JSON.stringify({
                "device_id" : "00:07:80:79:31:67",
                "services":[
                    {
                        "uuid":"08c8c7a0-6cc5-11e3-981f-0800200c9a66",
                        "characteristics":[
                            {"uuid":"50888c10-6cc5-11e3-981f-0800200c9a66","action":"read","value":""}
                        ]
                    } 
                  , {
                        "uuid":"d752c5fb-1380-4cd5-b0ef-cac7d72cff20",
                        "characteristics":[
                            {"uuid":"883f1e6b-76f6-4da1-87eb-6bdbdb617888","action":"read","value":""},
                            {"uuid":"21819AB0-C937-4188-B0DB-B9621E1696CD","action":"read","value":""},
                            {"uuid":"57b09eae-a369-4677-bf6e-92a95baa3a38","action":"read","value":""},
                            {"uuid":"6696f92e-f3c0-4d85-84dd-798b0ceff2e9","action":"read","value":""},
                            {"uuid":"488b0448-3aaf-44c6-90ec-c85fd5d9f616","action":"read","value":""},
                            {"uuid":"e101b160-a59b-4f24-97df-6821337b45b2","action":"read","value":""},
                            {"uuid":"7834933b-3f6d-44b3-b38e-8b67a7ed6702","action":"read","value":""},
                            {"uuid":"b08e1773-fbb6-428d-9e4c-d6322f7bf5fe","action":"read","value":""},
                            {"uuid":"c98e8fd3-5be7-43e0-b35a-951f1a07c25c","action":"write","value":1,"format":"uint8"},
                            {"uuid":"977840be-8d5c-4805-a329-2f9ddc8db3a3","action":"read","value":""},
                            {"uuid":"bb2ed798-9d0b-41a6-a753-31b16f93281e","action":"read","value":""},
                            {"uuid":"4a0efa07-e181-4a7f-bd72-094df3e97132","action":"read","value":""}
                        ]
                    }
                ]
                // "ui":"<html><body style='font-family:sans-serif-thin; font-size:20pt; background:#a00; color:#fff'>TESSEL<br /><br />Room Temperature : 78&deg;<br /><br />Humidity: 15%<br /><br />TIME:<br />10:30 AM</body></html>"
            }));
        } else {
            res.send("RE-RESPOND SUCCESSFUL! DISCONNECT BLE NOW!");
            data = {"ID":req.body.DEVICE_ID,"version":hex2a(req.body.ATTRIBUTES[0].value)}
            for (i=1; i<req.body.ATTRIBUTES.length; i++) {
                try {
                    var translated = hex2a(req.body.ATTRIBUTES[i].value).split('{')[1].split('}')[0].split(':');
                    data[translated[0].split("\"")[1].toLowerCase()] = translated[1];
                } catch(e) {
                    data[req.body.ATTRIBUTES[i].characteristic] = req.body.ATTRIBUTES[i].value;
                }
            }
            request.post({
                url:'http://gatd.eecs.umich.edu:8081/SgYPCHTR5a',
                headers:{"content-type":"application/json"},
                body:JSON.stringify(data)},
                function(error,response,body){console.log(body);});
        }


    /* OTHER */

    else res.send("");


    // For logging on demo site
    request.post({
        url:'http://gatd.eecs.umich.edu:8081/SgYPCHTR5a',
        headers:{"content-type":"application/json"},
        body:JSON.stringify(req.body)},
        function(error,response,body){console.log(error);});

});

module.exports = router;


// io.on('connection', function (socket) {
//   socket.emit('news', { hello: 'world' });
//   socket.on('my other event', function (data) {
//     console.log(data);
//   });
// })
