#!/bin/bash
export $(cat ../common/common.env | xargs)
export $(cat ../auth-service/.env | xargs)
mvn spring-boot:run