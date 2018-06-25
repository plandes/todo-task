## makefile automates the build and deployment for lein projects

# type of project, currently one of: clojure, python
PROJ_TYPE=		clojure
PROJ_MODULES=		nlpmodel appassem
# namespace is not templatized
GITUSER=		plandes
GITPROJ=		todo-task

# project specific
PAPER_DOC_SRC_DIR=	$(abspath $(DOC_SRC_DIR))
ADD_CLEAN=		results.xls $(PAPER_DOC_SRC_DIR)
DIST_PREFIX=		$(HOME)/opt/app

TODO_CONF=		resources/todocorp.conf
TODO_OP=		-c $(TODO_CONF)

# make build dependencies
_ :=	$(shell [ ! -d .git ] && git init ; [ ! -d zenbuild ] && \
	  git submodule add https://github.com/plandes/zenbuild && make gitinit )

include ./zenbuild/main.mk

.PHONY:	help
help:
		$(LEIN) run

.PHONY:	testprep
testprep:
		mkdir -p dev-resources
		mkdir -p results
		make models

.PHONY: test
test:		testprep
		$(LEIN) test

.PHONY:	startes
startes:
		make -C docker-es up

.PHONY:	stopes
stopes:
		make -C docker-es down

.PHONY:	load
load:		testprep
		$(LEIN) run load -l INFO $(TODO_OP)

.PHONY:	features
features:	testprep
		$(LEIN) run features -f 500 $(TODO_OP)

.PHONY:	dsprep
dsprep:		testprep
		$(LEIN) run dsprep -l INFO $(TODO_OP)

.PHONY:	print
print:		testprep
		$(LEIN) run print -l INFO $(TODO_OP)

.PHONY:	printbest
printbest:	testprep
		$(LEIN) run print -l INFO --metaset set-best --classifiers j48 $(TODO_OP)

.PHONY:	evaluate
evaluate:	testprep
		$(LEIN) run evaluate -l INFO $(TODO_OP)

.PHONY:	predict
predict:	testprep
		$(LEIN) run predict -l INFO $(TODO_OP)

.PHONY:	disttodo
disttodo:	dist
	cp $(TODO_CONF) $(DIST_DIR)
	cp src/bin/run.sh $(DIST_DIR)
# needed to silence a deeplearn4j exception
	mkdir -p $(DIST_DIR)/dev-resources
	mkdir -p $(DIST_DIR)/resources
	cp resources/todo-dataset.json $(DIST_DIR)/resources

.PHONY:	alldocs
alldocs:
	mkdir -p doc
	make FINAL_PDF_DIR=$(PAPER_DOC_SRC_DIR) -C ../../paper clean pdf
	make FINAL_PDF_DIR=$(PAPER_DOC_SRC_DIR) -C ../../slides clean pdf
	make docs
