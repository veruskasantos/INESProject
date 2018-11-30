// Script to run Waze Crawler
// Run node waze_crawler.js <name_of_the_cities>

const puppeteer = require('puppeteer');
const MongoClient = require('mongodb').MongoClient;
const fs = require('fs');

const url = 'mongodb://127.0.0.1:27017';
const dbName = 'waze_data';

setTimeout(function () {

	process.argv.forEach(function (arg, index, array) {
		(async () => {
			if (index > 1) {

				var city = arg
				var saveData = false

				let browser = await puppeteer.launch({headless: false});
				const page = await browser.newPage();

				await page.on('response', response => {
					try {
						if (saveData) {
							if (response.url().indexOf("TGeoRSS") > -1) {
								response.json().then(resp => { 
									MongoClient.connect(url, function(err, client) {
										console.log("Connected successfully to server");
										const db = client.db(dbName);
										const types = ['jams', 'alerts'];

										types.forEach((collection_type) => {
											let collection = db.collection(`waze_${city}_${collection_type}`);
											if (resp[collection_type] !== undefined && resp[collection_type] !== null) {
												resp[collection_type] = resp[collection_type].filter((el) => {
													return (el !== (undefined || null || ''));
												});
												resp[collection_type].forEach(rec => {
													rec['_id'] = rec['id'] 
													collection.update({'_id': rec['_id']}, 
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

		  // process.on("unhandledRejection", (reason, p) => {
		  //   console.error("Unhandled Rejection at: Promise", p, "reason:", reason);
		  //   browser.close();
		  // });
		  console.log(arg)
		  await page.setViewport({width: 1280, height: 1200, deviceScaleFactor: 1});
		  await page.goto('https://www.waze.com/pt-BR/livemap');

		  //name of the input the receive the text
		  let txt = "input[class='wm-search__input']"
		  await page.waitFor(txt);
		  await page.focus(txt);
		  
		  //coordinates does not work anymore with waze
		  await page.evaluate(() => {
		  	document.querySelector("input[class='wm-search__input']").value = ''
		  });

		  await page.type(txt, city, {delay: 10});
		  await page.waitFor(500);
		  await page.keyboard.press('ArrowDown');
		  await page.waitFor(500);
		  await page.keyboard.press('Enter');
		  await page.waitFor(1500);

		  let zoomOut = 'a.leaflet-control-zoom-out';
		  
		  // ajustando a visão do mapa para pegar o máximo possível
		  if (city == 'Curitiba') {
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
		  	await page.waitFor(500);
		  	await page.keyboard.press('ArrowDown')

		  	saveData = true
		  	console.log('Saving' + city)
		  	await page.waitFor(2000);

		  } else if (city == 'Recife') {
		  	await page.click(zoomOut);
		  	await page.waitFor(500);
		  	await page.click(zoomOut);
		  	await page.waitFor(500);
		  	await page.click(zoomOut);
		  	await page.waitFor(500);
		  	await page.keyboard.press('ArrowUp')

		  	saveData = true
		  	console.log('Saving' + city)
		  	await page.waitFor(2000);

		  } else if (city == 'Campina-Grande') {
		  	await page.click(zoomOut);
		  	await page.waitFor(500);
		  	await page.click(zoomOut);

		  	saveData = true
		  	console.log('Saving' + city)
		  	await page.waitFor(2000);

		  } else {
		  	console.log('${city} city not found.')
		  }

		  await browser.close();

	} //end city if
})();


});

}, 300000);

