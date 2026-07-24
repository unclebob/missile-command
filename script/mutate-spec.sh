#!/usr/bin/env bash
# Speclj runner for clj-mutate workers. Works from the project root or from a
# worker overlay directory that only has src/ + spec/ (no deps.edn).
set -euo pipefail
exec clojure \
  -Sdeps '{:paths ["src" "spec"]
           :deps {org.clojure/clojure {:mvn/version "1.12.0"}
                  org.clojure/data.json {:mvn/version "2.5.1"}
                  speclj/speclj {:mvn/version "3.12.2"}}}' \
  -M -m speclj.main -c
