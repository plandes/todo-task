# Supervised Approach Imperative To-Do List Categorization

This repository contains a corpus and code base to categorize natural language
todo list items as described in our paper [A Supervised Approach To The
Interpretation Of Imperative To-Do Lists].

This repository contains:

* A publicly available [corpus](#corpus).
* A [code base](#code-base) similar to that given as published results in the
  [arXiv paper].


<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
## Table of Contents

- [Documents](#documents)
- [Corpus](#corpus)
- [Citation](#citation)
- [Code Base](#code-base)
    - [What's Included](#whats-included)
    - [Third Party Libraries](#third-party-libraries)
    - [Documentation](#documentation)
- [Running the Tests](#running-the-tests)
    - [Parsing](#parsing)
    - [Test Evaluation](#test-evaluation)
    - [Predictions](#predictions)
    - [Off-line Tests](#off-line-tests)
    - [Sample Results of Test](#sample-results-of-test)
    - [Building](#building)
    - [Advanced](#advanced)
- [Changelog](#changelog)
- [Special Thanks](#special-thanks)
- [License](#license)

<!-- markdown-toc end -->


## Documents

* [Corpus]
* [Paper on arXiv] (please [cite](#citation) this paper)
* [Paper](https://plandes.github.io/todo-task/SupervisedInterpretationImperativeToDos.pdf) (please do **not**
  cite this paper).
* [Slides](https://plandes.github.io/todo-task/SupervisedInterpretationImperativeToDosSlides.pdf)
* [Evaluation](results/full-evaluation.xls) (generated using
  the [evaluation functionality](#test-evaluation))
* [Predictions](results/predictions.csv) (generated using
  the [predictions functionality](#predictions))


## Corpus

The publicly available corpus is available [here](resources/corpus.xlsx) in
Excel format.  This corpus is referred to as *Corpus B* in the [arXiv
paper]. The columns in the spreadsheet are:

| Column Name   | Description                                     | Trello Artifact |
|---------------|-------------------------------------------------|-----------------|
| `utterance`   | The natural language todo list text.            | no              |
| `class`       | The label if classified, otherwise left blank.  | no              |
| `board_name`  | The name of the board                           | yes             |
| `board_id`    | The board ID                                    | yes             |
| `short_url`   | The URL of the comment on Trello                | yes             |
| `description` | Additional description information for the task | yes             |


## Citation

Please use the following to cite the [arXiv paper].

```jflex
@article{landesDiEugenio2018,
  title = {A Supervised Approach To The Interpretation Of Imperative To-Do Lists},
  url = {http://arxiv.org/abs/1806.07999},
  note = {arXiv: 1806.07999},
  journal = {arXiv:1806.07999 [cs]},
  author = {Landes, Paul and Di Eugenio, Barbara},
  year = {2018},
  month = {Jun}
}
```

If you use this software in your research, please cite with the following
BibTeX (note that the [third party libraries] also have citations):

```jflex
@misc{plandesTodoTask2018,
  author = {Paul Landes},
  title = {Supervised Approach Imperative To-Do List Categorization},
  year = {2018},
  publisher = {GitHub},
  journal = {GitHub repository},
  howpublished = {\url{https://github.com/plandes/todo-task}}
}
```


## Code Base

The code base used in this repository is an updated version of the code used on
*Corpus A* (see the [arXiv paper]).  It is written in [Clojure] and written to
be accessed mostly via `make` commands.  However, it can be compiled into a
command line app if you want to run the long running cross fold validation
tasks.  See the [Running the Tests](#running-the-tests) to compile and run it.


### What's Included

The functionality included is *agent classification* as described in the [arXiv
paper].  The following is *not* included:

* Argument classification
* Extending the Named Entity Recognizer (section 4.1)
* The first verb model (section 4.2)

This functionality is not included as the origianl code base is proprietary.
This code base was rewritten and [third party libraries] utilized where
possible to speed up the development.


### Third Party Libraries

Primary libraries used are listed below.  Their dependencies can be traced from
their respective repo links:

* [Natural Language Parsing and Feature Generation]
* [Interface for Machine Learning Modeling]
* [Generate, split into folds or train/test and cache a dataset]
* [Natural Language Feature Creation]
* [Word Vector Feature Creation]


### Documentation

API [documentation](https://plandes.github.io/todo-task/codox/index.html).


## Running the Tests

This section explains how to run the the model against the corpus to reproduce
the results (*similar*) to the [arXiv paper].  These instructions assume either
a UNIX, Linux, macOS operating system or *maybe* Cygwin under Windows.

Before proceeding, please install all the all tools given in
the [building](#building) section.


### Parsing

This section describes how to parse the corpus and load the corpus.  Note that
if you just want to run the tests you can **skip**
to [test evaluation](#test-evaluation) section.  This means you don't need
[ElasticSearch], which is only necessary for parsing the corpus and creating
file system train/test split.  This is already done
and [in the repo](resources/todo-dataset.json) already.

On the other hand, if you **really** want to manually parse and create the
train/test data sets you must first install [ElasticSearch] or [Docker].  The
easiest way to get this up and working is to use [Docker], which is easy enough
to download, install and get running on a container with:

```bash
make startes
```

which provides the configuration necessary to download and start an
[ElasticSearch] container ready to store the generated features from the parsed
natural language text.

Next, populate [ElasticSearch] with parsed featues:

```bash
make load
```

This parses the corpus and adds a JSON parse representation of each utterance
to the database.

Next create train and test datasets by randomly shuffling the corpus.  After
the train/test assignment for each data point, export the data set to the JSON
file:

```bash
make dsprep
```


### Test Evaluation

Produce the optimal results for the model by evaluating and
printing the results:

```bash
make printbest
```

This gives the best (0.76 F1) results.


To run all defined feature and classifier combinations run the following:

```bash
make print
```


To run all defined feature and classifier combinations and create a spreadsheet
with all performance metrics, features and classifiers used for those metrics run:

```bash
make evaluate
```

This will create an `evaluation.xls` file.  The file this process generates
is [here](results/full-evaluation.xls).


### Predictions

It is possible to generate a CSV file with predictions complete with the
utterance, the correct label, and the predicted label.  In addition, the file
also includes all features used to create the prediction.  This proces includes:

1. For each feature sets and classifier combination, train the model and test.
2. The winning combination (by F1) of feature set and classifier is used to
   train the model.
3. Create predictions on the test set.
4. Generate the spreadhsheet with the results.

To invoke this functionality, use the following:

```bash
make predict
```

This will generate a `predictions.csv` file.  The file this process generates
is [here](results/predictions.csv).


### Off-line Tests

If you have a slower computer and the tests take too long, they can run in an
offline mode.

To long running offline tests in the background, first download and link to the
models (note the space between `ZMODEL=` and `models` is intentional):

```bash
make ZMODEL= models
```

create the application as a standalone and then
execute in the background:

```bash
make ZMODEL=`pwd`/model DIST_PREFIX=./inst disttodo
cd ./inst/todotask
./run.sh sanity
tail -f log/test-res.log
```

Type `CONTROL-C` to break out of `tail` and check open `results/test-res.xlsx`
to confirm the a single line from a simple majority label classifer (it will
have terrible performance).

If everything works, now run the long running tests:

```bash
./run.sh long
ls results
```

The `results` directory will have the results from each test.
Section [results](#results-of-code-base) has a summary of each test.


### Sample Results of Test

A selection of results using the this code base on [*Corpus B*] are given
below:

|  Classifier  | F1        | Precision | Recall |                                                                                                                                              Attributes                                                                                                                                              |
|--------------|----------:|----------:|-------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| J48          |   0.763 | 7677 | 0.761 | similarity-top-label, pos-last-tag, word-count-contact, word-count-call, word-count-buy, word-count-calendar, word-count-pay-bill-online, pos-first-tag, word-count-plan-meal, word-count-email, word-count-postal, word-count-school-work, word-count-print                                         |
| RandomForest |   0.695 | 7101 | 0.714 | *all word counts*, elected-verb-id, similarity-top-label, similarity-score, pos-tag-ratio-noun                                  |
| RandomTree   |   0.656 | 6654 | 0.666 | *all word counts*, elected-verb-id, similarity-top-label, similarity-score, pos-tag-ratio-noun                                  |
| LogitBoost   |   0.592 | 6171 | 0.619 | *all word counts*, elected-verb-id, similarity-top-label, similarity-score, pos-tag-ratio-noun                                  |
| NaiveBayes   |   0.547 | 5779 | 0.571 | similarity-top-label, pos-last-tag, word-count-contact, word-count-call, word-count-buy, word-count-calendar, word-count-pay-bill-online, pos-first-tag, word-count-plan-meal, word-count-email, word-count-postal, word-count-school-work, word-count-print                                         |
| SVM          |   0.273 | 2815 | 0.285 | elected-verb-id, token-average-length, pos-first-tag, pos-last-tag, similarity-top-label, similarity-score, pos-tag-ratio-noun                                                                                                                                                                       |
| Baseline     |   0.091 | 2356 | 0.238 | similarity-top-label, pos-last-tag, word-count-contact, word-count-call, word-count-buy, word-count-calendar, word-count-pay-bill-online, pos-first-tag, word-count-plan-meal, word-count-email, word-count-postal, word-count-school-work, word-count-print                                         |


### Building

To build from source, do the folling:

- Install [Leiningen](http://leiningen.org) (this is just a script)
- Install [GNU make](https://www.gnu.org/software/make/)
- Install [Git](https://git-scm.com)
- Download the source: `git clone --recurse-submodules https://github.com/plandes/todo-task && cd todo-task`


### Advanced

All the capabilities of the [Interface for Machine Learning Modeling] package,
including creating a usable executable model, are possible.  The (not unit test
case) [Clojure] [experimental execution file](test/uic/nlp/todo/eval_test.clj)
demonstrates how to do other things with the model.  All you need to do is to
start a [REPL](https://clojure.org/guides/repl/introduction) and call the
`main` function.


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## Special Thanks

Thanks to those that volunteered their To-do tasks that, in part, made
up this publicly available corpus.


## License

This license applies to the code base and the corpus.

Copyright (c) 2018 Paul Landes

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


<!-- links -->
[A Supervised Approach To The Interpretation Of Imperative To-Do Lists]: https://arxiv.org/pdf/1806.07999
[arXiv paper]: https://arxiv.org/pdf/1806.07999
[Paper on arXiv]: https://arxiv.org/pdf/1806.07999
[*Corpus B*]: resources/corpus.xlsx
[Corpus]: resources/corpus.xlsx

[ElasticSearch]: https://www.elastic.co
[Docker]: https://www.docker.com
[Clojure]: https://clojure.org

[Natural Language Parsing and Feature Generation]: https://github.com/plandes/clj-nlp-parse
[Interface for Machine Learning Modeling]: https://github.com/plandes/clj-ml-model
[Generate, split into folds or train/test and cache a dataset]: https://github.com/plandes/clj-ml-dataset
[Natural Language Feature Creation]: https://github.com/plandes/clj-nlp-feature
[Word Vector Feature Creation]: https://github.com/plandes/clj-nlp-wordvec
[third party libraries]: #third-party-libraries
