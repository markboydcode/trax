#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
java -Dloader.main=nbdp.trax.mcp.TraxMcpServer -jar "$DIR/target/trax-6.0.0.jar"