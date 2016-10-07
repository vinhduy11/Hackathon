#!/bin/bash
protoc  --cpp_out=./cpp --java_out=../java  SoapProto.proto
