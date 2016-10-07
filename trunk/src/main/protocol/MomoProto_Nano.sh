#!/bin/bash
protoc --javanano_out='enum_style=java,java_multiple_files=true,enum_style=java,optional_field_style=accessors:.' ./nano MomoProto.proto
