#!/bin/bash
export $(cat ../common/common.env | xargs)
mvn spring-boot:run