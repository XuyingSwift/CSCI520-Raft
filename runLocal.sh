#!/bin/bash

echo -n "Enter Node ID: "
read nodeId

configStr="0 127.0.0.1 6001 1 127.0.0.1 6002 2 127.0.0.13 6003 3 127.0.0.15 6005 4 127.0.0.1 6006"
args="$nodeId $configStr"
echo $args

cd out/production/csci-520_raft_consensus/
java RaftRunner $args
java RaftRunner $args