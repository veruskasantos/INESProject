// Script to run Waze Crawler
// Run node waze_crawler.js <interval_in_minutes> <name_of_the_cities>
// Ex: node waze_crawler.js 5 Curitiba Recife Campina-Grande

const puppeteer = require('puppeteer');
const MongoClient = require('mongodb').MongoClient;
const fs = require('fs');

const url = 'mongodb://127.0.0.1:27017';
const dbName = 'waze_data';
const milliseconds = 60000

argsList = process.argv.slice(2)
interval_threshold = argsList[0] * milliseconds //convert to milliseconds

if (argsList.length < 2) {
	console.log('You should run: node <script_name.js> <interval_threshold_in_minutes> <name_of_the_cities>')
	process.exit(1)
}

setInterval(function () {

	date = new Date()
	current_data = date.toString().slice(0, 24)
	current_data_day = date.getDate() + '-' + (date.getMonth()+1) + '-' + date.getUTCFullYear()
	
	argsList.forEach(function (arg, index, array) {
		(async () => {

			if (index > 0) { /// get name of the cities

				var city = arg
				var saveData = false

				//to see browser execution: headless: false
				let browser = await puppeteer.launch({headless: true});
				const page = await browser.newPage();

				await page.on('response', response => {
					try {
						if (saveData) {
							if (response.url().indexOf("TGeoRSS") > -1) {
								response.json().then(resp => { 
									MongoClient.connect(url, { useNewUrlParser: true }, function(err, client) {
										console.log("Connected successfully to server");
										const db = client.db(dbName);
										const types = ['jams', 'alerts'];

										types.forEach((collection_type) => {
											let collection = db.collection(`waze_${city}_${collection_type}_${current_data_day}`);
											if (resp[collection_type] !== undefined && resp[collection_type] !== null) {
												resp[collection_type] = resp[collection_type].filter((el) => {
													return (el !== (undefined || null || ''));
												});
												resp[collection_type].forEach(rec => {
													rec['_id'] = rec['id'] 
													collection.updateOne({'_id': rec['_id']}, 
														{'$set': rec}, {upsert: true});
												});
											}
										});

										client.close();
									});
								});
							}
						}
					} catch(ex) {
						console.log(ex);
						browser.close();
					}
				});

				console.log('Getting data: ' + arg + ' - ' + current_data)

				await page.setViewport({width: 1280, height: 1200, deviceScaleFactor: 1});
				await page.goto('https://www.waze.com/pt-BR/livemap');

				//name of the input to receive the text
				let txt = "input[class='wm-search__input']"
				await page.waitFor(txt);
				await page.focus(txt);

				// coordinates does not work anymore with waze
				// await page.evaluate(() => {
				// 	document.querySelector("input[class='wm-search__input']").value = 'Curitiba'
				// 	});

				await page.click("#map > div > div > div > div > div > input[class='wm-search__input']")
				await page.type(txt, city, {delay: 100});
				await page.waitFor(500);

				// curitiba: ChIJ3bPNUVPj3JQRCejLuqVrL20
				// recife: ChIJ5UbEiG8ZqwcR1H9EIin1njw
				// cg: ChIJhRwgQ18erAcRUa-mOuxqZck
				// wm-search__dropdown > wm-search_item / ul > li

				// await page.keyboard.press('ArrowDown');
				// await page.waitFor(500);
				// await page.keyboard.press('Enter');
				// await page.waitFor(1000);

				let zoomOut = 'a.leaflet-control-zoom-out';

				// ajustando a visão do mapa para pegar o máximo possível
				if (city == 'Curitiba') {
					const elementHandle = await page.$('[data-value="ChIJ3bPNUVPj3JQRCejLuqVrL20"]');
					await elementHandle.click();
					await page.waitFor(1000);

				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowDown')
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowDown')
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowDown')
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowDown')

				  	console.log('Saving ' + city)
				  	saveData = true
				  	await page.waitFor(2000);

				  } else if (city == 'Recife') {
				  	const elementHandle = await page.$('[data-value="ChIJ5UbEiG8ZqwcR1H9EIin1njw"]');
					await elementHandle.click();
					await page.waitFor(1000);

				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowLeft')
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowLeft')

				  	console.log('Saving ' + city)
				  	saveData = true
				  	await page.waitFor(2000);

				  } else if (city == 'Campina-Grande') {
				  	const elementHandle = await page.$('[data-value="ChIJhRwgQ18erAcRUa-mOuxqZck"]');
					await elementHandle.click();
					await page.waitFor(1000);

				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.click(zoomOut);
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowLeft')
				  	await page.waitFor(500);
				  	await page.keyboard.press('ArrowDown')

				  	console.log('Saving' + city)
				  	saveData = true
				  	await page.waitFor(2000);

				  } else {
				  	console.log(city + ' city not found.')
				  	console.log('You should run: node <script_name.js> <interval_threshold_in_minutes> <name_of_the_cities>')
				  }

				  await browser.close();

			} //end city if
		})();


	});

}, interval_threshold);
