#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
java -cp "$DIR/target/trax-5.0.0-uber.jar" nbdp.trax.mcp.TraxMcpServer -cfg "$DIR/spring-cfg.xml"