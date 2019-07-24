import numpy as np
import pickle
import os
import pandas as pd

filename = '../Combined Trajectory_Label_Geolife/Revised_InstanceCreation+NoJerkOutlier+NOSmoothing.pickle'
# Each of the following variables contain multiple lists, where each list belongs to a user
with open(filename, 'rb') as f:
    Total_RelativeDistance, Total_Speed, Total_Acceleration, Total_Jerk, Total_BearingRate, Total_Label, Total_InstanceNumber, Total_Instance_InSequence, Total_Delta_Time, Total_Velocity_Change = pickle.load(f, encoding='latin1')

# Y: Labels
Y = []

Dist = []
AV = []
EV = []
VV = []
MaxV1 = []
MaxV2 = []
MaxV3 = []
MaxA1 = []
MaxA2 = []
MaxA3 = []
HCR = []  # Heading Change Rate
SR = []  # Stop Rate
VCR = []  # Velocity Change Rate
HC = 19  # Heading rate threshold
VS = 3.4  # Stop rate threshold
VR = 0.26  # VCR threshold

for k in range(len(Total_InstanceNumber)):

    RD = Total_RelativeDistance[k]
    SP = Total_Speed[k]
    VC = Total_Velocity_Change[k]
    AC = Total_Acceleration[k]
    J = Total_Jerk[k]
    BR = Total_BearingRate[k]
    DT = Total_Delta_Time[k]
    LA = Total_Label[k]
    # IN: the instances and number of GPS points in each instance for each user k
    IN = Total_InstanceNumber[k]
    for i in range(len(IN)):
        end = IN[i]
        if end == 0 or sum(RD[i]) == 0:
            continue
        # Basic features
        # Dist: Distance of segments
        Dist.append(sum(RD[i]))
        # AV: average velocity
        AV.append(sum(RD[i])/sum(DT[i]))
        # EV: expectation velocity
        EV.append(np.mean(SP[i]))
        # VV: variance of velocity
        VV.append(np.var(SP[i]))
        # MaxV1, MaxV2, MaxV3
        sorted_velocity = sorted(SP[i], reverse=True)
        MaxV1.append(sorted_velocity[0])
        MaxV2.append(sorted_velocity[1])
        MaxV3.append(sorted_velocity[2])
        # MaxA1, MaxA2, MaxA3
        sorted_acceleration = sorted(AC[i], reverse=True)
        MaxA1.append(sorted_acceleration[0])
        MaxA2.append(sorted_acceleration[1])
        MaxA3.append(sorted_acceleration[2])
        # Heading change rate (HCR)SR+HCR+VCR
        Pc = sum(1 for item in BR[i] if item > HC)
        HCR.append(Pc * 1./sum(RD[i]))
        # Stop Rate (SR)
        Ps = sum(1 for item in SP[i] if item < VS)
        SR.append(Ps * 1. / sum(RD[i]))
        # Velocity Change Rate (VCR)
        Pv = sum(1 for item in VC[i] if item > VR)
        VCR.append(Pv * 1. / sum(RD[i]))
        # Segment Label
        Y.append(LA[i])

X = [Dist, AV, EV, VV, MaxV1, MaxV2, MaxV3, MaxA1, MaxA2, MaxA3, HCR, SR, VCR, Y]
X = np.array(X, dtype=np.float32).T

df = pd.DataFrame(X)
print(df)
df.to_csv('Hand_Crafted_features.csv', index=False, header=['Distance', 'Average Velocity', 'Expectation Velocity',
                                           'Variance of Velocity', 'MaxV1', 'MaxV2', 'MaxV3', 'MaxA1', 'MaxA2', 'MaxA3',
                                           'Heading Rate Change', 'Stop Rate', 'Velocity Change Rate', 'Label'])