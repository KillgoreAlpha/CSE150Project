#!/bin/bash
ssh-keygen -t ecdsa -C "$1"
cat ~/.ssh/id_ecdsa.pub