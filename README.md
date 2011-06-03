# Class Reload Agent

Monitors Java class files in the file system for changes and reloads them into
a running Java virtual machine.  Use this during development to reload classes
instead of redeploying an application.

This is a fork of [AgentSmith](http://java.net/projects/agentsmith) modified to
monitor multiple class directories.

The implementation uses HotSwap, so it can reload a class only if a method body
was changed.  It cannot reload a class if a field or method was added or
deleted.  Buy [JRebel](http://www.zeroturnaround.com/jrebel/) if you want
something more capable.  The expense is more than made up by the savings in
developer time.


## Usage

Add this Java virtual machine command line option:

    -javaagent:/path/to/class-reload-agent.jar=classes=classpath

where *classpath* is a list of directories separated by the system-dependent
path-separator.  On UNIX systems, the path-separator is `:`.  On Windows
systems, it is `;`.
