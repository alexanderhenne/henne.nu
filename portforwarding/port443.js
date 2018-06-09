var natpmp = require('nat-pmp');

// create a "client" instance connecting to your local gateway
var client = natpmp.connect('192.168.1.1');


// explicitly ask for the current external IP address
client.externalIp(function (err, info) {
  if (err) throw err;
  console.log('Current external IP address: %s', info.ip.join('.'));
});

portMap();

setInterval(function(){
	portMap();
}, 2 * 60 * 60 * 1000);      

function portMap() {
	client.portMapping({ private: 443, public: 443, ttl: 7200, type: 'tcp' }, function (err, info) {
		if (err) throw err;
		console.log(info);
	});
}