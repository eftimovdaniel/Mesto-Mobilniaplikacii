#!/bin/bash
# Двојно клик во Finder → се отвора Terminal и се пушта API.
cd "$(dirname "$0")" || exit 1
echo "Стартува mesto API од $(pwd) ..."
npm start
