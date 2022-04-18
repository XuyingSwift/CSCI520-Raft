#!/bin/bash

echo -n "Enter Node ID: "
read nodeId

configStr="0 18.212.28.37 4000 1 52.207.209.55 4000 2 44.204.63.193 4000 3 18.118.51.255 4000 4 54.219.59.46 4000"
args="$nodeId $configStr"
echo $args

echo -n "Enter Robot ID: "
read robotId

configStr="5 34.222.100.173 4000 6 34.222.79.72 4000"
args="$robotId $configStr"
echo $args

cd out/production/csci-520_raft_consensus/
java RaftRunner $args