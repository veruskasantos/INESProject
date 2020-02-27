For each city, run:

1.PRE-PROCESSING
GPS
Recife
- Separate files per day
- Convert coordinates
- Remove data without linea, lat, lon

Curitiba
- Convert JSON to CSV file


WAZE
- Remove lines from different city
- Order files fields


WEATHER
- Separate files per day
- Change lat/lon fields


GTFS
- Label routes.txt with route frequency


-> Run Data_Integration_script


2. INTEGRATION (java)
GPS/GTFS
- Generate stop_times_shapes
- Generate shapes_STREET

-> Run MergeGTFSFiles
-> Run MatchingGPSShapeStop
-> Run MatchingGPSWeatherWaze
-> Run HeawayLabeling


3. FEATURE ENGINEERING
- Generate new feats
- Normalize all data

-> Run
-> Run


3. PREDICTION
- Make bus bunching predictions

-> Run