// Script to get data from mongodb and to save as CSV in local path
// Run: 
// --> cd ...workspace/Crawler/
// --> node get_data_from_mongodb.js <name_database>

// Ex (all files): node get_data_from_mongodb.js waze_data Curitiba Recife Campina-Grande
// Ex (one file): node get_data_from_mongodb.js waze_data waze_Curitiba_jams_11-2-2019

var mongo = require('mongodb').MongoClient;
var assert = require('assert');
const fs = require('fs');
const json2csv = require('json2csv').parse;

var url = 'mongodb://127.0.0.1:27017'

argsList = process.argv.slice(2)
if (argsList.length < 1) {
	console.log('You should run: node <script_name.js> <name_database>')
	process.exit(1)
}

var dbName = argsList[0];

var resultArray = [];
mongo.connect(url, function(err, client) {
	assert.equal(null, err);
	const db = client.db(dbName);

	//To get one file
	if (argsList.length == 2) {
		var collection_name = argsList[1]
		console.log('Converting file ' + collection_name)
		
		var cursor = db.collection(collection_name).find();
		cursor.forEach(function(doc, err) {
			assert.equal(null, err);
			resultArray.push(doc);

		}, function() {
			db.close;
			//save file
			fs.writeFile("data/" + dbName + "/" + collection_name + ".csv", convertJSON2CSV(resultArray),
			 function(err) {
				assert.equal(null, err);
				console.log("The file was saved!");
			});
		})

	// To get all files
	} else {
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
						fs.writeFile("data/" + dbName + "/" + collectionName + ".csv", convertJSON2CSV(resultArray),
						 function(err) {
							assert.equal(null, err);
							console.log("The file was saved!");
						});
					})
				})
			
			
		}, function() {
			client.close();
		});
	}
})


function convertJSON2CSV(data, dbName) {
	const fields_waze = ['_id', 'severity', 'country', 'city', 'level', 'line', 'speedKMH', 'length', 
	'turnType','type', 'uuid', 'endNode', 'speed', 'segments', 'roadType', 'delay', 'updateMillis', 
	'street', 'id', 'pubMillis'];

	const fields_gps = ['id' , 'stopSequence', 'diffLastUpdate', 'bearing', 'vehicleLabel', 'stopId', 'lon', 
	'tripId', 'positionTime', 'routeId', 'delay', 'arrivalTime', 'vehicleId', 'tripStartTime', 'tripHash',
	'percTravel', 'lat'];

	var opts = ''
	if (dbName == 'waze_data') {
		opts = { fields_waze };
	} else {
		opts = { fields_gps };
	}

	try {
	  const csv = json2csv(data, opts);
	  return csv
	} catch (err) {
	  console.error(err);
	}
}