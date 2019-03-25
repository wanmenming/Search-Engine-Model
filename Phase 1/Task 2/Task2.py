import math
import operator
from bs4 import BeautifulSoup
import os
import re


# Jelinek-Mercer Smoothing
# p(qi|D) = (1-lambda)fqi/|D| + lambda*cqi/|C|

# lambda value
const_lamba = 0.35

# dictionary of cacm queries
queries_dict = {}

# dictionary for corpus term frequencies
corpus_dict = {}

# dictionary for document length
doc_length_dict = {}

# dictionary for inverted index
inverted_index_dict = {}

# average corpus length
corpus_size = 0

# dictionary of expanded queries created after pseudo relevance
new_queries = {}


# Pick top 10 queries and perform query expansion using pseudo relevance feedback
def pseudo_relevance_query_expansion(query, query_id, ranked_table):
    term_freq_dict = {}
    for doc, score in ranked_table:  # for each document in top 10 results
        for term, doc_freq in inverted_index_dict.items():
            if doc in inverted_index_dict[term]:  # if the document contains the term, get its frequency
                freq = inverted_index_dict[term][doc]

                if term in term_freq_dict:  # update term frequencies
                    term_freq_dict[term] += int(freq)
                else:
                    term_freq_dict[term] = int(freq)

    # sort term frequencies in descending order
    sorted_terms = sorted(term_freq_dict.items(), key=operator.itemgetter(1))

    # select top 12 terms
    top5 = sorted_terms[::-1][:12]

    # create expansion queries with these new terms
    for term, score in top5:
        if query_id in new_queries:
            new_queries[query_id] += " " + term
        else:
            new_queries[query_id] = query + " " + term


# Calculates JM Smoothed Likelihood score for each query
def jm_smoothed_likelihood(queries, output_file, is_pseudo_rel_run):
    for query_id, query in queries.items():
        query = query.strip()
        ' '.join(query.split())
        regex = r"(?<![a-z])[-.]?[\d]+([,.][\d]+)*|[a-zà-γ\d]+([-][a-zà-γ\d]+)*"  # process the query in the same was the corpus is processed
        query_terms = re.finditer(regex, query, re.UNICODE | re.IGNORECASE)
        term_count = dict()
        scores = dict()

        for term in query_terms:  # for each query term calculate query frequency
            term = term.group().lower()
            if term in term_count:
                term_count[term] = term_count[term] + 1
            else:
                term_count[term] = 1

        for term, term_freq in term_count.items():  # for each query term

            if term in inverted_index_dict:
                cq = int(corpus_dict[term])  # get corpus frequency of the term

                for doc_id in doc_length_dict:   # check if the query term exists in the document
                    if doc_id in inverted_index_dict[term]:
                        dq = inverted_index_dict[term][doc_id]  # term exists in the document - get its frequency
                    else:
                        dq = 0  # term does not exist in the document, set frequency to 0

                    prob_term_given_doc = calculate_term_score(doc_id, dq, cq)  # calculate JM smoothing for the term

                    if doc_id in scores:  # collect all the scores for each document
                        scores[doc_id] = scores[doc_id] + prob_term_given_doc
                    else:
                        scores[doc_id] = prob_term_given_doc

        # sort scores in descending order
        scores_sorted = sorted(scores.items(), key=operator.itemgetter(1), reverse=True)

        if is_pseudo_rel_run:
            # get the pseudo relevance feedback  of the top 10 results
            ranked_scores = scores_sorted[::-1][:10]
            pseudo_relevance_query_expansion(query, query_id, ranked_scores)
        else:
            # print top 100 scores to the file
            write_to_file(scores_sorted, query_id, output_file)


# To write the list to file
def write_to_file(scores, query_id, path):
    path = path + "JMSmoothedScoreWithPseudoRelevance_Query_" + query_id

    # # if the file already exists, open it in append mode
    # if os.path.exists(path):
    #     append_write = 'a'  # append if already exists
    # else:  # else, create it and write to it
    #     append_write = 'w'  # make a new file if not

    count = 1
    output_file = open(path, 'w')
    for doc_id, score in scores:
        if count > 100:
            break
        output_file.write("%s Q0 %s %s %s JMQueryLikelihood_PseudoRelevance\n" % (query_id, doc_id, count, score))
        count += 1


# Calculates JM Smoothed Query Likelihood score for each term in the query
# dq = document frequency of the term
# cq = corpus frequency of the term
def calculate_term_score(doc_id, dq, cq):
    part_1 = ((1 - const_lamba) * (float(dq) / float(doc_length_dict[doc_id])))
    part_2 = const_lamba * (float(cq) / float(corpus_size))
    prob_term_given_doc = part_1 + part_2

    prob_term_given_doc = math.log(prob_term_given_doc)

    return prob_term_given_doc


# Creates the dictionary with query Id and queries
def create_query_dict(location):
    data = open(location, 'r')
    soup = BeautifulSoup(data, 'lxml')  # parse xml
    docs = soup.find_all('doc')

    for doc in docs:
        doc_no = doc.find('docno')  # extract query id
        key = doc_no.text.strip()  # remove extra spaces and blank lines

        if len(doc_no) > 0:
            doc.find('docno').decompose()  # remove tag docno

        value = doc.text.strip()  # extract query text and remove extra spaces and blank lines
        queries_dict[key] = value


# Creates the dictionary for term and its frequency in the corpus
def create_corpus_tf(location):
    data = open(location, 'r')
    count = 1

    for line in data:
        if count == 1:  # skip the heading
            count += 1
            continue

        tf = line.strip("\n").split(" ")
        corpus_dict[tf[0]] = tf[1]


# Creates the dictionary for document ids and their length
def create_doc_length_dict(location):
    data = open(location, 'r')
    count = 1
    global corpus_size

    for line in data:
        if count == 1:  # skip the header
            count += 1
            continue
        tf = line.strip("\n").split(" ")
        doc_length_dict[tf[0]] = tf[2]  # save document length for each document
        corpus_size += int(tf[2])  # update corpus length


# Create the dictionary for the inverted index
def create_inverted_index_dict(location):
    count = 1
    data = open(location, 'r')

    for line in data:
        if count == 1:  # skip the header
            count += 1
            continue
        document_frequency_map = {}

        data = line.split(":")  # split each line in the file as (term, information)
        term = data.pop(0)

        tokens = data.pop(0).split(" ")  # split into => size of inverted list (document_id, frequency)
        tokens.pop(0)  # remove "size of inverted list" from the token list

        for token in tokens:
            docid_freq = token.split(",")  # split into (document_id, frequency)
            doc_id = docid_freq.pop(0).lstrip('(')  # get doc ID
            freq = docid_freq.pop(0).rstrip(')\n')  # get term frequency for the doc ID
            document_frequency_map[doc_id] = freq

        inverted_index_dict[term] = document_frequency_map


# Start J M Smoothed Likelihood query calculation with pseudo relevance feedback
def main():
    # location for cacm.query.txt file
    cacm_queries_location = "D:\Manpreet\sem4\IR\project\\test-collection\cacm.query.txt"

    # location for unigrams inverted indexes with term frequency
    inverted_index_location = "D:\Manpreet\sem4\IR\project\\Number_Tables_Removed\inverted_lists\inverted_list_term_freqency.txt"

    # location for corpus statistics file
    tf_location = "D:\Manpreet\sem4\IR\project\\Number_Tables_Removed\inverted_lists\corpus_statistics.txt"

    # location for document statistics files
    doc_stats_location = "D:\Manpreet\sem4\IR\project\\Number_Tables_Removed\inverted_lists\document_statistics.txt"

    # outfile file location
    output_file = "D:\Manpreet\\repo\IR-Project\phase1\Task2\output\\"

    create_query_dict(cacm_queries_location)
    create_inverted_index_dict(inverted_index_location)
    create_corpus_tf(tf_location)
    create_doc_length_dict(doc_stats_location)

    # First run to get the pseudo relevance feedback and perform query expansion
    jm_smoothed_likelihood(queries_dict, "", True)

    # Second run to get JM Smoothed Likelihood scores for expanded queries
    jm_smoothed_likelihood(new_queries, output_file, False)


if __name__ == '__main__':
    main()

