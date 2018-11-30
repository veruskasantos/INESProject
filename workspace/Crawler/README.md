To start MondoDB, run in terminal:
-> sudo service mongod start (Ubuntu)
-> mongod.exe (Windows)

To start crawler of traffic data, run in terminal:
-> node waze_crawler.js <name_of_the_cities>

When finished Crawler, stop MongoDB:
-> sudo service mongod stop

-------------------------------------------

To see data in MongoDB, run:
-> mongo
-> show databases
-> use waze_data
-> show collections
-> db.waze_ctb_alerts.find() (to see data in terminal)
-> 