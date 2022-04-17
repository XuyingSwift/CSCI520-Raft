#!/bin/bash

echo -n "Enter Node ID: "
read nodeId

configStr="0 127.0.0.1 6000 1 127.0.0.1 6001 2 127.0.0.1 6002"
args="$nodeId $configStr"
echo $args

cd out/production/csci-520_raft_consensus/
java RaftRunner $args