# Phyloptimize
This Python implementation of the Geophylogenetic Tree optimization algorithm (Phyloptimize) can be used either from the command line, if you just want to optimize trees for yourself or as a webserver. This webserver offers a number of features, as can be seen by this instance of it: *(https://url.of.instance)*

Make sure you have Docker and Docker Compose installed and running *(https://docs.docker.com/desktop/)*

First, the proper environment variables have to be set in any case.

## Environemt Variables

Depending on your use case, a number of environment variables are relevant. The EXAMPLE.env lists them and explains which variables are relevant for you.

There are multiple ways to set these variables. The way choosen here, is via a ".env" file that get's passed to docker. Simply adjust the necessary variables in the EXAMPLE.env file and rename it to ".env"

## How To Use As Webserver

If you want to use Phyloptimize as a webserver, you can simply pull the pre-build Docker containers we uploaded to Dockerhub via (on some machines, instead of "docker compose" the command is "docker-compose")

    docker compose pull

and start them via.

    docker compose up

Then visit http://localhost:5000/ (the port varies, depending on the environmet variable).

In case you want to build the containers locally, instead of pulling from Dockerhub, you can simply rename the "docker-compose-build.yml" to "docker-compose.yml" and then run

    docker-compose up --build

## How To Use From The Command Line

This readme explains how to set up the Phyloptimize CLI in a Docker container. If you want to install the CLI natively, you can also do so.

Pull the repository and navigate to its root folder. Then build the container with:

    docker build -f DockerfileCLI -t phyloptimize_cli .

And afterwards run and connect to it via:

    docker run -v $(pwd)/input:/app/input -v $(pwd)/output:/app/output --env-file .env -it phyloptimize_cli bash

This command mounts the folders "output" and "input" to the container, so files saved in there are transferred between the container and the host machine. Additionally, it starts the container and connects you to it via a command line.

In the container, you can now parse, optimize and draw the geo-trees as shown with the example files. The first python script "parseFiles.py" takes a tree file in newick format and a map file as either a csv or a geojson and creates a single instance json file, that will later be given to the optimizer. The parser can be run with:

    python python/parseFiles.py input/example_tree_1.dnd input/example_map_1_named.geojson -o output/example_instance.json

If the output parameter -o is omitted, the result will be printed to the console.

To optimize the instance, you have to run:

    python python/optimize.py output/example_instance.json -o output/example_solution.json

Depending on the size of the instance, this step might take a while and will save the optimized configuration of the tree in a solution json file under the specified path -o.

Finally, to draw the optimized tree, you have to run the script "drawPhylogeo.py" with the instance and solution file as parameters. The -o parameter again defines the path where the drawing should be saved.

    python python/drawPhylogeo.py output/example_instance.json output/example_solution.json -o output/example_drawing.svg

Check the --help section of the three commands to see extra options to configure your tree. For example, adding the parameter 

    -l po

to the three commands will change to parallel-orthogonal leaders, instead of the default straight leaders and adding

    -bm osmEmbed

to the drawPhylogeo.py script will change the background to a static map.

As the output folder is mounted to the host machine, you should now see the svg file outside of your container and be able to view it with any svg-viewer.