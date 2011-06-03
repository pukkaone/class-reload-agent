# Class Reload Agent

Monitors Java class files in the file system for changes and reloads them into
a running Java virtual machine.

This is a fork of [AgentSmith](http://java.net/projects/agentsmith) modified
to monitor multiple class directories.


## Usage

Add this Java virtual machine command line option:

    -javaagent:/path/to/class-reload-agent.jar=classes=*classpath*

where *classpath* is a list of directories separated by the system-dependent
path-separator.  On UNIX systems, the path-separator is `:`.  On Windows
systems, it is `;`.
