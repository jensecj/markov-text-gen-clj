# markov-text-gen

Simple text generation with markov-chains.

## Usage
Create an uberjar with `lein uberjar`, copy the jar to the source of the project
(it needs the `resources` folder with books).

Run the jar `java -jar markov-text-gen-0.1.0-SNAPSHOT-standalone.jar`.

The program will then:

1) load the books from `resources`, convert them into individual markov chain
files, which will be saved in `markov-files/`.

2) merge them together into a single file `merged.markov`, which will also be
saved.

3) load the merged file, and create a sentence from provided seed words.
