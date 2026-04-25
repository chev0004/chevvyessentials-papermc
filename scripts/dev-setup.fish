#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set props_file "$project_root/gradle.properties"

set paper_dir ""
if test -n "$argv[1]"
    set paper_dir "$argv[1]"
else if test -n "$PAPER_RUN_DIRECTORY"
    set paper_dir "$PAPER_RUN_DIRECTORY"
else if test -f "$props_file"
    set paper_dir (rg '^paperRunDirectory=' "$props_file" | string replace 'paperRunDirectory=' '')
end

if test -z "$paper_dir"
    echo "Missing Paper run directory." >&2
    echo "Usage: fish scripts/dev-setup.fish /path/to/paper-server" >&2
    echo "Or set PAPER_RUN_DIRECTORY or paperRunDirectory=... in gradle.properties." >&2
    exit 1
end

if not test -f "$paper_dir/paper.jar"
    echo "Could not find paper.jar in: $paper_dir" >&2
    exit 1
end

cd "$project_root"

./gradlew build
or exit 1

set jar_candidates (rg --files build/libs -g 'chevvyessentials-*.jar' | string match -rv '(-sources|-javadoc)\.jar$')
set jar $jar_candidates[1]
if test -z "$jar"
    echo "No plugin jar found under build/libs" >&2
    exit 1
end

mkdir -p "$paper_dir/plugins"
set old_plugin_jars (rg --files "$paper_dir/plugins" -g 'chevvyessentials-*.jar')
if test (count $old_plugin_jars) -gt 0
    rm -f $old_plugin_jars
end

cp -f "$jar" "$paper_dir/plugins/chevvyessentials.jar"
echo "Installed: $jar -> $paper_dir/plugins/chevvyessentials.jar"

cd "$paper_dir"
exec java -jar paper.jar --nogui
