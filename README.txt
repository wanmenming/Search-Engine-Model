This project is split between 2 languages: Java and Python.

* The Java Project should be run first in order to generate the output files used for the rest
of the project. The java project handles the following:

    1. Generating the corpus from `cacm/*`, the stemmed corpus from `cacm_stem.txt`, the inverted indexes and
    statistic files for `cacm/*`, the inverted indexes and statistic files for `cacm_stem.txt`, the lucene
    corpus from `cacm` (lightly processed to remove extraneous numbers at the end of files), and the lucene indexes
    for both the normal and stemmed corpus.
    2. Implementations for BM25 w/stop word support, Lucene w/ stop word support.
    3. Extra-Credit: Implementation for proximity matching, exact matching, and best match.
        Please see java section for examples!

* The python files can be found in each of the phase folders and contains the implementations for the following:
    1. Implementation for JM Smoothed Query Likelihood Model.
    2. Implementation for Query Enrichment.
    3. Implementation for Snippet Retrieval (Phase 2).
    4. Implementation for TF-IDF (including with stop words and stemming).
    5. Implementation for Phase 3 evaluation.

Java Setup Instructions:

  1. This project depends on maven version 3.x.x (I used 3.5.4). Please follow the download instructions from `http://maven.apache.org/install.html`.
  Or if you have `homebrew` installed, run `brew install maven`.

  2. This project requires java version 1.8.

  3. From the project root on the command line, install the following jars a total of 4:

  `mvn install:install-file -Dfile=./src/libs/lucene-analyzers-common-7.5.0.jar -DgroupId=org.apache -DartifactId=lucene-analyzers-common -Dversion=7.5.0 -Dpackaging=jar`.

  `mvn install:install-file -Dfile=./src/libs/lucene-queryparser-7.5.0.jar -DgroupId=org.apache -DartifactId=lucene-queryparser -Dversion=7.5.0 -Dpackaging=jar`.

  `mvn install:install-file -Dfile=./src/libs/lucene-core-7.5.0.jar -DgroupId=org.apache -DartifactId=lucene-core -Dversion=7.5.0 -Dpackaging=jar`.

  4. Once maven is installed, navigate to the project root from the terminal and run the command
  `mvn clean install`.

  5. In order to run the app use the following command:
  `java -jar target/information-retrieval-1.0-SNAPSHOT.jar`

  Example for extra credit. Note, queries should be wrapped in quotes if you want to include spaces:
  -i or -id e.g. -id="1" (required)
  -q or -query e.g. -query="this is a sample query" (required)
  -t or -type e.g. -type=exact (required) Possible types are bestmatch or exact or proximity
  -p or -proximity e.g. -proximity=5 (required if type = proximity, must be a positive integer)

  Sample usages:
    java -jar target/information-retrieval-1.0-SNAPSHOT.jar -query="this is a query" -i=1 -t=proximity -p=10
    java -jar target/information-retrieval-1.0-SNAPSHOT.jar -query="this is a query" -i=4 -t=exact
    java -jar target/information-retrieval-1.0-SNAPSHOT.jar -query="this is a query" -i=4 -t=bestmatch

    Output files will be:
    BM_25_Best_Match_With_Proximity_<N>_Results_For_Query_<ID>.txt for proximity queries w/ best match
    BM_25_Exact_Search_Results_For_Query_<ID>.txt for exact queries
    BM_25_Results_For_Query_<ID>.txt for best match

  6. Main class is `IRProject.java`. If making code changes, run the command `mvn package` to compile new jar file.

  7. To change the location of the cacm directory, stemmed directory, etc. modify lines 16 - 20, to
  your desired paths.

Python Setup Instructions:

    1. Both python projects depend on python version 3.7.0.

    2. `Pycharm` ide was used.

    3. Download and install the following libraries and include them in pycharm:

    `BeautifulSoup4 4.6.3`[https://pypi.org/project/beautifulsoup4/]

    `nltk 3.3` [https://www.nltk.org/]

    `lxml 4.2.5` [https://pypi.org/project/lxml/]


### Source Code Python

* JMLikelihoodSmoothing.py
	* Calculates the JM Smoothed Query Likelihood score for the CACM queries
	* Usage:
		* cacm_queries_location: location location for cacm.query.txt file
		* inverted_index_location: location for unigrams inverted indexes with term frequency
		* tf_location: location for corpus statistics file
		* doc_stats_location: location for document statistics files
		* output_file: location where output files will be generated
* Task2.py
	* Calculates the JM Smoothed Query Likelihood score for the CACM queries with Pseudo Relevance Feedback
	* Usage:
		* cacm_queries_location: location location for cacm.query.txt file
		* inverted_index_location: location for unigrams inverted indexes with term frequency
		* tf_location: location for corpus statistics file
		* doc_stats_location: location for document statistics files
		* output_file: location where output files will be generated
* GenerateSnippet.py
	* Generates snippets from the original corpus documents for the JM Smoothed Query Likelihood score documents
	* Usage:
		* corpus_location: location of the corpus
		* cacm_queries_location: location location for cacm.query.txt file
		* tf_location: location for corpus statistics file
		* jm_score_location: location for JM Smoothed Query Likelihood scores
		* output_file: location where output files will be generated

* evaluation.py
    * Evaluate eight distinct runs with results of all 64 queries
    * Usage:
        * cacm_rel_location: location for cacm.query.txt file
        * input_location:  location for Top100 of all models' result,
                           each model's files should be a sub folder. The name of sub-folder should be model name
        * output_table: location where output file of all the precision & recall table will be generated
        * output_file_plot: location where output file of the precision & recall (by interpolation ) will be generated. We draw the Recall-Precision
                            curve based by the interpolated value
        * output_evaluation: location where output files of MAP MRR P@K will be generated
        * output_pk: location where output files of P@K for each query of 8 models will be generated

* tf-idf.py
    * Calculates TF-IDF scores
    * Usage:
        * cacm_queries_location: location location for cacm.query.txt file
        * inverted_index_location: location for unigrams inverted indexes with term frequency
        * tf_location: location for corpus statistics file
        * doc_stats_location: location for document statistics files
        * output_file: location where output files will be generated

* tf-idf-task3.py
    * uses common words and stemming
    * Usage:
        * corpus_location: location of the corpus
        * cacm_queries_location: location location for cacm.query.txt file
        * cacm_stem_queries_location: location location for cacm.stem.query.txt file
        * common_words_location: location location for common_words file
        * output_file: location where output files will be generated

### Outputs Python

* Phase 1
	* Task 1
		* Filename: JMLikelihoodSmoothing.py
		* Results: phase1/task1/output
        * Filename: tf-idf.py
        * Results: phase1/task1/outputTFIDF
	* Task 2
		* Filename: Task2.py
		* Results: phase1/task2/output
    * Task 3
        * Filename: tf-idf-task3.py
        * Results: A - phase1/task2/outputA
                   B - phase1/task2/outputB

* Phase 2
	* Filename: GenerateSnippet.py
	* Results: phase2/output

* Phase 3
	* Filename: evaluation.py
	* Results:1,2,3 -  phase3/evaluation
	          4 - phase3/outputtable
	          5 - phase3/outputplot