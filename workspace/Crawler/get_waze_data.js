//Script to get waze data from mongodb and to save as CSV in local path
// Run get_waze_data.js

var mongo = require('mongodb').MongoClient;
var assert = require('assert');
const fs = require('fs');
const json2csv = require('json2csv').parse;

var url = 'mongodb://127.0.0.1:27017'
const dbName = 'waze_data';

var resultArray = [];
mongo.connect(url, function(err, client) {
	assert.equal(null, err);
	const db = client.db(dbName);

	db.listCollections().toArray(function(err, collections) {
		assert.equal(null, err);
		console.log(collections)

		collections.forEach(function(name, err) {
			var collectionName = name['name']
			console.log('Collection: ' + collectionName); // print the name of each collection
			
			var cursor = db.collection(collectionName).find(); //and then print the json of each of its elements}
			cursor.forEach(function(doc, err) {
				assert.equal(null, err);
				resultArray.push(doc);

			}, function() {
				db.close;
				//save file
				fs.writeFile("data/waze_data/" + collectionName + ".csv", convertJSON2CSV(resultArray),
				 function(err) {
					assert.equal(null, err);
					console.log("The file was saved!");
				});
			})
		})
	}, function() {
		client.close();
	});

})


function convertJSON2CSV(data) {
	const fields = ['_id', 'severity', 'country', 'city', 'level', 'line', 'speedKMH', 'length', 
	'turnType','type', 'uuid', 'endNode', 'speed', 'segments', 'roadType', 'delay', 'updateMillis', 
	'street', 'id', 'pubMillis'];
	const opts = { fields };

	try {
	  const csv = json2csv(data, opts);
	  return csv
	} catch (err) {
	  console.error(err);
	}
}