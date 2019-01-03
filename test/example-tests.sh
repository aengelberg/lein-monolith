#!/bin/bash

# Fail if any subcommand fails
set -e

REPO_ROOT="$(cd $(dirname "${BASH_SOURCE[0]}")/.. && pwd)"
PLUGIN_VERSION="$(head -1 project.clj | cut -d ' ' -f 3)"
echo "Installing lein-monolith $PLUGIN_VERSION from source..."
cd $REPO_ROOT
lein install

EXAMPLE_DIR="${REPO_ROOT}/example"
cd $EXAMPLE_DIR
echo
echo "Updating example project to use lein-monolith version $PLUGIN_VERSION..."
sed -i'.bak' -e "s/lein-monolith \"[^\"]*\"/lein-monolith $PLUGIN_VERSION/" project.clj

echo
echo "Running tests against example projects in $EXAMPLE_DIR"

test_monolith() {
    echo
    echo -e "\033[36mlein monolith $@\033[0m"
    lein monolith "$@"
    echo
}

test_monolith info
test_monolith lint
test_monolith deps-of example/app-a
test_monolith deps-on example/lib-a
test_monolith with-all cprint :dependencies :source-paths
test_monolith each cprint :version
test_monolith each :in lib-a cprint :root
test_monolith each :upstream-of lib-b cprint :version
test_monolith each :downstream-of lib-a cprint :name
test_monolith each :parallel 3 :report :endure cprint :group
test_monolith each :refresh foo install
test_monolith each :refresh foo install
test_monolith each :parallel 3 :refresh bar install
test_monolith fingerprint info
test_monolith fingerprint clear :upstream-of lib-b
test_monolith fingerprint mark :upstream-of lib-b foo bar
