Data: https://drive.google.com/file/d/1NJJKGFeHxHAgXfX7tOIbML9H3r-HKEfW/view?usp=sharing (integrated data of Curitiba - output of steps 1 and 2)

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
- Order files fields
- Remove lines from different city


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

Add outputs folder in Data Analysis/data/output/<city>/

3. FEATURE ENGINEERING
- Generate new feats
- Normalize all data

-> Run Feature_Engineering

-> Run Converting_Data


3. PREDICTION
- Separate data by days
- Make bus bunching predictions

-> Run Data
-> Run models
