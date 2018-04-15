#!/bin/bash
# Restore the .gnupg folder if it is not present
mvn -Prelease-sign-artifacts clean deploy
