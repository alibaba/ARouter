#!/bin/bash

dx --dex --output patch.dex classes

adb push patch.dex /data/local/tmp/arouter/external/patch.dex