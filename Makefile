# Preamble
SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

# Project
SHELL:=/bin/bash
ROOT_DIR:=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))


release:
	sbt +clean +release
.PHONY: release

releaseSnapshot:
	sbt +publishSigned
.PHONE: releaseSnapshot
