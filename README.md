# ROS-CAGG template package

This repository contains a simple node that performs text recognition based on the Concept-Action Grammar Generator API ([CAGG](https://github.com/EmaroLab/concept_action_grammar_generator)).

### Dependences

It depends on ros-java, installable o via `apt-get`  (tested with Ubuntu 16.04 and ROS kinetic) and the CAGG API.
The latter is included as a jar file in the `/lib` folder, run it with a JVM if you what to use the CAGG grammar generator GUI.

This repository has been based on the ROS-java templated package [available here](https://github.com/buoncubi/ros_java_template_pkg)

### Installation

Simpli copy this repository in the `src` folder of your *catking workspace*. Run `$ catkin_make`, and than run `./gradlew deployApp` in the `ros_cagg_node` package.

### Execution

An example can be executed using:
``
roslaunch ros_cagg_teleop ros_cagg_interface.launch
``

## Architecture

The repository provides a node (called `CAGG_node` and implemented in `ros_java_cagg_node_interface`) which listens to a topic and publish in another. 
The definitions of the ROS messages are stored in a separate package called `ros_cagg_msgs`.
In particular,
- the input topic is named `/CAGG/input_text/` and is a string (i.e., a text provided by a user)
- the output topic is a custom message called `cagg_tags` and structured as:
 - `Header`,
 - `Time Stamp`,
 - `cagg_tag` (a list of list of string, where each recognized words in a sentences are defined through a list of semantic tag, based on the provided CAGG grammar),
 - `confidence` (a `float` value between 0 and 1).

### Parameter

The `CAGG_node` requires specific value on the ROS parameter server, in particular:
- `/cagg_log_config_path`: the absolute path to the Log4j configuration file,
- `/cagg_serialized_directive_grammar`: the absolute path tho the serialized CAGG grammar file about *directives* commands,
- `/cagg_serialized_go_grammar`: the absolute path tho the serialized CAGG grammar file about *go* commands,

Also, you can set other further parameters of `CAGG_node` such as:
- `/cagg_timeout_ms`: the time (in milliseconds) to run the CAGG evaluation, after which the best result so far will be published in the output topic (remarkably, the evaluation start as soon as you send something on the input topic). By default set to 10000ms.
- `/cagg_stopping_check_frequency`: represent the frequency of stopping condition used before time-out. Remarkably, time-out will be applied after a multiple times of the `/cagg_stopping_check_frequency`. By default set to 1000ms.
- `/cagg_stopping_confidence_threshold`: represents the confidence threshold to stop searching for further results before time-out.
 - the *confidence* is computed as the ration of the word in a sentence at which CAGG attached at least a semantic tag, over the total number of words in a sentence. Therefore, if you grammar is not accurate (i.e., do not catch all the words in sentence), the confidence will be low even if a suitable recognition as been found.

### Behavior

This package implements a simple behavior based on the `res/hello_world.cagg`.
But you can use it as a starting point for building something more clever

### Author

[Luca Buoncompagni](mailto:luca.buoncompagni@edu.unige.it)
EMAROlab, DIBRIS department, University of Genoa, Italy.
