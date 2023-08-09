if [ ! -n "$1" ] ;then
    echo "Do not specify release version!"
    exit 1;
fi;

echo "release version: $1"
mvn versions:set -DnewVersion=$1
export GPG_TTY=$(tty) && mvn clean deploy -P ossrh,release
git add -u
git commit -m "release: v-$1"
git tag -a "v-$1" -m "release version $1"
git push --tags