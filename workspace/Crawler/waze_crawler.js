const puppeteer = require('puppeteer');
const MongoClient = require('mongodb').MongoClient;
const fs = require('fs');

const url = 'mongodb://127.0.0.1:27017';
const dbName = 'waze_data';

(async () => {
  let browser = await puppeteer.launch({headless: false});
  const page = await browser.newPage();
  //await page.setRequestInterception(true);
  await page.on('response', response => {
    try{
        if (response.url().indexOf("TGeoRSS") > -1){
            response.json().then(r => { 
            MongoClient.connect(url, function(err, client) {
                console.log("Connected successfully to server");
                const db = client.db(dbName);
                const types = ['jams', 'alerts'];
		
                types.forEach((t) => {
                    let collection = db.collection(`waze_ctb_${t}`);
                    if (r[t] !== undefined && r[t] !== null){
                        r[t] = r[t].filter((el) => {
                            return (el !== (undefined || null || ''));
                        });
                        r[t].forEach(rec => {
                            rec['_id'] = rec['id'] 
                            collection.update({'_id': rec['_id']}, 
                                        {'$set': rec}, {upsert: true});
                        });
                    }
                });
                
                client.close();
                //console.log('Done')
            });
            //fs.writeFileSync(`dados_${new Date().getTime()}`, r);
            });
        }
    }catch(ex) {
       console.print(ex);
       browser.close();
    }
  });

  // process.on("unhandledRejection", (reason, p) => {
  //   console.error("Unhandled Rejection at: Promise", p, "reason:", reason);
  //   browser.close();
  // });

  await page.setViewport({width: 1280, height: 1200, deviceScaleFactor: 1});
  await page.goto('https://www.waze.com/pt-BR/livemap');

  //name of the input the receive the text
  let txt = "input[class='wm-search__input']"
  await page.waitFor(txt);
  await page.focus(txt);
  //await page.type(txt, 'Curitiba, State of Paraná, Brazil', {delay: 1});
  
  //coordinates does not work anymore with waze
  await page.evaluate(() => {
                document.querySelector("input[class='wm-search__input']").value = ''
          });
  //TODO receive name of the citis by command line
  //ajustar o script para executar para cada cidade (sequencialmente?)
  //ajustar o banco para salvar separado para cada cidade
  await page.type(txt, `Curitiba PR`, {delay: 10});
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown');
  await page.waitFor(1000);
  await page.keyboard.press('Enter');
  await page.waitFor(2000);

  // ajustando a visão do mapa para pegar o máximo possível
  let zoomOut = 'a.leaflet-control-zoom-out';
  await page.click(zoomOut);
  await page.waitFor(1000);
  await page.click(zoomOut);
  await page.waitFor(1000);
  await page.click(zoomOut);
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown')
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown')
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown')
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown')
  await page.waitFor(1000);
  await page.keyboard.press('ArrowDown')
  await page.waitFor(2000);


  //let zoomOut = 'a.leaflet-control-zoom-out';
  //await page.click(zoomOut);
  /*for(let i = 0; i < 1; i++){
    setTimeout(() => { page.click(zoomOut); }, 1000 * (1 + i));
  }*/
  await browser.close();
})();
