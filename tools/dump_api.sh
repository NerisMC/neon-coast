#!/bin/bash
# Prints the public/protected API of the loader classes we use, so code can be written against the exact versions.
set -u
out=dump; mkdir -p $out
dumpjar() {   # $1 = jar, $2 = output name, $3 = package regex
  local jar=$1 name=$2 pat=$3
  unzip -Z1 "$jar" 2>/dev/null | grep '\.class$' | grep -v 'module-info' | sed 's/\.class$//; s#/#.#g' | grep -E "$pat" > $out/$name-classes.txt
  if [ -s $out/$name-classes.txt ]; then
    xargs -a $out/$name-classes.txt -n 200 javap -protected -cp "$jar" >> $out/$name.txt 2>/dev/null
  fi
}
for p in neoforge forge fabric; do
  [ -f $p/build/cp.txt ] || continue
  while read -r jar; do
    case "$jar" in
      *neoforge*|*NeoForge*) dumpjar "$jar" "neo-$(basename $jar .jar)" '^net\.neoforged\.(neoforge\.(event|client|registries|common|network|capabilities)|bus|fml)' ;;
      *forge*) dumpjar "$jar" "forge-$(basename $jar .jar)" '^net\.minecraftforge\.(event|client|registries|common|network|fml|eventbus|server)' ;;
      *eventbus*|*bus-*) dumpjar "$jar" "bus-$(basename $jar .jar)" '.' ;;
      *fml*|*loader*) dumpjar "$jar" "fml-$(basename $jar .jar)" '(fml|loading)\.' ;;
    esac
  done < $p/build/cp.txt
done
ls -la $out | head -50
