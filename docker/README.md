# Docker runtime environment

This small project provides a Docker setup of the dependencies required to run Graylog:

* Elasticsearch
* MongoDB


## Docker

__NOTE:__ On Mac OS X and Windows you might need to install [Docker](https://www.docker.com/docker-engine),  
[Docker Machine](https://docs.docker.com/machine/), and [Docker Compose](https://docs.docker.com/compose/) (or simply
install [Docker Toolbox](https://www.docker.com/docker-toolbox) to get all of these).

Most commands can be run using the `do` script in this directory. Run `./do help` for a list of commands and their
descriptions. The commands for the `do` script are inspired by the Docker Compose commands, so take a look at those
as well.


### Docker Machine (Mac OS X and Windows)

Once `docker-machine` is available on the machine, the prepared virtual machine ("default" in this example) can be
started using the following commands:

    # Start virtual machine "default"
    docker-machine start default
    
    # Read in environment variables for virtual machine "default"
    eval "$(docker-machine env default)"


The IP address of the virtual machine running the Docker daemon can be retrieved with the following command:

    docker-machine ip default


This IP address (or a hostname pointing to that IP address) should be used in the Graylog configuration.
In environments which can run Docker natively (i. e. Linux), the IP addresses of the containers can be retrieved using
the following command (in which `${CID}` is the container ID):

    docker inspect -f '{{ .NetworkSettings.IPAddress }}' ${CID}


### Volumes

This runtime environment is using [Docker Volumes](https://docs.docker.com/engine/userguide/dockervolumes/) to persist
data from its containers.
Those volumes can be created by using the [`docker volume create`](https://docs.docker.com/engine/reference/commandline/volume_create/)
command.

By default, the volumes `graylog-elasticsearch` and `graylog-mongodb` are being used.
In order to bind these volumes to the Docker containers, they have to be created by using the command `./do mkvol`.

Once those volumes aren't required anymore or should be recreated, simply run `./do rmvol`.


### Create, start, stop, and remove containers

In order to create and start the Docker containers simply run `./do up`. Most settings can be overridden by environment
variables, see `config/do.conf` for a list of those.

Already existing containers can be started by running `./do start` and stopped by running `./do stop`.
Docker containers can also be paused and unpaused with `./do pause` and `./do unpause` respectively.

Existing Docker containers can be removed with `./do rm`.
