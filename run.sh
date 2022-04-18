#!/bin/bash

echo -n "Enter Node ID: "
read nodeId

configStr="0 127.0.0.1 5000 1 127.0.0.1 5001 2 127.0.0.1 5002 3 127.0.0.1 5003 4 127.0.0.1 5004"
args="$nodeId $configStr"
echo $args

echo -n "Enter Robot ID: "
read robotId

configStr="5 34.222.100.173 4000 6 34.222.79.72 4000"
args="$robotId $configStr"
echo $args

cd out/production/csci-520_raft_consensus/
java RaftRunner $args
